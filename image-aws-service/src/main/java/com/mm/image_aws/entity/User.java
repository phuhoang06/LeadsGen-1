package com.mm.image_aws.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections; // === THÊM MỚI ===
import java.util.Date;


@Entity
@Table(name = "TB_USER",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "S_USERNAME"),
                @UniqueConstraint(columnNames = "S_EMAIL")
        })
@Data
@ToString
@EqualsAndHashCode
public class User implements Serializable, UserDetails {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid.hex")
    @Column(name = "S_ID", length = 32)
    private String s_id;

    @Column(name = "S_USERNAME", length = 50, nullable = false)
    private String username;

    @Column(name = "S_EMAIL", length = 255, nullable = false)
    private String email;

    @Column(name = "S_PASSWORD", length = 100, nullable = false)
    @JsonIgnore
    private String password;

    @Column(name = "S_NAME", length = 256)
    private String name;

    @Column(name = "S_STATE", length = 32, nullable = false)
    private String state = "active";


    @Column(name = "D_CREATE", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date d_create = new Date();

    @Column(name = "D_UPDATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date d_update;

    public String getS_id() {
        return s_id;
    }

    // === SỬA LỖI: Trả về một danh sách rỗng thay vì null ===
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }
    // =======================================================

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "active".equalsIgnoreCase(this.state);
    }
}
