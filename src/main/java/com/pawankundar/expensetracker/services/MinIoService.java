package com.pawankundar.expensetracker.services;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pawankundar.expensetracker.domain.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xmlpull.v1.XmlPullParserException;

import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.NoResponseException;

@Service
public class MinIoService {

    @Autowired
    MinioClient minioClient2;

    
    public void saveObject(String bucketName, String objectName, InputStream stream, long size, String contentType)
            throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, NoResponseException,
            ErrorResponseException, InternalException, InvalidArgumentException, InsufficientDataException, IOException,
            XmlPullParserException, InvalidPortException, InvalidEndpointException {
            getMinioClient().putObject(bucketName, objectName, stream, size, contentType);
    }

    public InputStream getObject(String bucketName, String objectName)
            throws InvalidPortException, InvalidEndpointException, IOException, InvalidKeyException,
            NoSuchAlgorithmException, InsufficientDataException, InternalException, NoResponseException,
            InvalidBucketNameException, XmlPullParserException, ErrorResponseException, InvalidArgumentException {
        return getMinioClient().getObject(bucketName, objectName);
    }
    
    public InputStream updateObject(String bucketName, String objectName,User newUser)
            throws InvalidPortException, InvalidEndpointException, IOException, InvalidKeyException,
            NoSuchAlgorithmException, InsufficientDataException, InternalException, NoResponseException,
            InvalidBucketNameException, XmlPullParserException, ErrorResponseException, InvalidArgumentException {


                //get old data from object
               InputStream oldData = getMinioClient().getObject(bucketName, objectName);
               ObjectMapper objectMapper = new ObjectMapper();
               objectMapper.registerModule(new JavaTimeModule());


               //append the new data to the old object
               List<User> userList = objectMapper.readValue(oldData, new TypeReference<List<User>>(){});
               userList.add(newUser);

               //convert the json to bytes and write
               ObjectMapper objectMapper2 = new ObjectMapper(); 
               byte[] bytesToWrite = objectMapper2.writeValueAsBytes(userList);

               saveObject("noonetestbucket2", objectName, new ByteArrayInputStream(bytesToWrite), bytesToWrite.length, "application/json");


        return getMinioClient().getObject(bucketName, objectName);
    }

    public MinioClient getMinioClient() throws InvalidPortException, InvalidEndpointException {
        System.out.println("called to create client");
        return new MinioClient("https://play.min.io", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
    }
}

