package com.mm.user.repo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.mm.user.entity.User;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public UserRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public User save(User user) {
        dynamoDBMapper.save(user);
        return user;
    }

    private Optional<User> findByAttribute(String indexName, String attributeName, String attributeValue) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val", new AttributeValue().withS(attributeValue));

        DynamoDBQueryExpression<User> queryExpression = new DynamoDBQueryExpression<User>()
                .withIndexName(indexName)
                .withConsistentRead(false)
                .withKeyConditionExpression(attributeName + " = :val")
                .withExpressionAttributeValues(eav);

        List<User> users = dynamoDBMapper.query(User.class, queryExpression);
        return users.stream().findFirst();
    }

    public Optional<User> findByUsername(String username) {
        return findByAttribute("username-index", "username", username);
    }

    public Optional<User> findByEmail(String email) {
        return findByAttribute("email-index", "email", email);
    }

    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }
}
