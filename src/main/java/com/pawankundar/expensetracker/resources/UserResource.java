package com.pawankundar.expensetracker.resources;


import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.rmi.ServerException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.pawankundar.expensetracker.Constants;
import com.pawankundar.expensetracker.domain.User;
import com.pawankundar.expensetracker.services.MinIoService;
import com.pawankundar.expensetracker.services.UserService;
import com.pawankundar.expensetracker.utils.Aes256;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xmlpull.v1.XmlPullParserException;

import de.taimos.totp.TOTP;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.NoResponseException;

@RestController
@RequestMapping("/api/users")
public class UserResource {

    @Autowired
    UserService userService;

    @Autowired
    MinIoService minIoService;
    

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody Map<String, Object> userMap) {
        String firstName = (String) userMap.get("firstName");
        String lastName = (String) userMap.get("lastName");
        String email = (String) userMap.get("email");
        String password = (String) userMap.get("password");
        User user = userService.registerUser(firstName, lastName, email, password);

        return new ResponseEntity<>(generateJWT(user), HttpStatus.OK);

    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody Map<String, Object> userMap) {
        String email = (String) userMap.get("email");
        String password = (String) userMap.get("password");
        Boolean generateQr = (Boolean) userMap.get("generateQr");

        // User user = userService.validateUser(email, password);

        Map<String, String> map = new HashMap<>();

       String key =  generateSecretKey(password);
       UUID uuid = UUID.randomUUID();  
       String secretKey = "Thisissecret";

        map.put("password", password);
        map.put("encryptedPassword", Aes256.encrypt(password));
        map.put("decryptedPassword", Aes256.decrypt(Aes256.encrypt(password)));
        map.put("Secret key",Aes256.encrypt(uuid.toString()+":D/7KAadCS9kLFgaYM7UGMGzlyt8UnCumTML1PRuaD5c=:"+secretKey));
        map.put("new key",generateSecretKey(Aes256.encrypt(uuid.toString()+":D/7KAadCS9kLFgaYM7UGMGzlyt8UnCumTML1PRuaD5c=:"+secretKey)));
        map.put("key", key);
        map.put("Authenticator code", getTOTPCode(key));
        map.put("uuid", uuid.toString());

        List<String> roles = new ArrayList<>();
        roles.add("admin");
        System.out.println(roles); 
        String authorities = String.join(",", roles);
        System.out.println(authorities); 

        //generating qr code
        if(generateQr){
            try {
                String companyName = "Pawan's Company";
                String barCodeUrl = getGoogleAuthenticatorBarCode(key, email, companyName);
                System.out.println(barCodeUrl);
                createQRCode(barCodeUrl,"C:\\TOTP\\qrCode.png",200,200);
            } catch (WriterException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new ResponseEntity<>(map, HttpStatus.OK);
    }

    private Map<String, String> generateJWT(User user) {
        long timestamp = System.currentTimeMillis();
        String token = Jwts.builder().signWith(SignatureAlgorithm.HS256, Constants.API_SECRET_KEY)
                .setIssuedAt(new Date(timestamp))
                .setExpiration(new Date(timestamp + Constants.TOKEN_VALIDITY))
                .claim("userId", user.getUserId())
                .claim("email", user.getEmail())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .compact();

        Map<String, String> map = new HashMap<>();
        map.put("username", token);
        return map;

    }


    //save object
    @GetMapping("/test")
    public void testApi () throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException, NoSuchAlgorithmException, ServerException, IllegalArgumentException, IOException, InvalidBucketNameException, NoResponseException, InvalidArgumentException, XmlPullParserException, InvalidPortException, InvalidEndpointException{
        User user = new User(123, "firstName", "lastName", "email", "password");
        List <User> userList = new ArrayList<>();
        userList.add(user);
        ObjectMapper objectMapper = new ObjectMapper(); 
        byte[] bytesToWrite = objectMapper.writeValueAsBytes(userList);
        minIoService.saveObject("noonetestbucket2", "testabcd.json", new ByteArrayInputStream(bytesToWrite), bytesToWrite.length, "application/json");
    }

    //read value get object
    @GetMapping("/test2")
    public User testApi2 () throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException, NoSuchAlgorithmException, ServerException, IllegalArgumentException, IOException, InvalidBucketNameException, NoResponseException, InvalidArgumentException, XmlPullParserException, InvalidPortException, InvalidEndpointException{
       InputStream userFromMinIo = minIoService.getObject("noonetestbucket2", "arrayUsers.json");
       ObjectMapper objectMapper = new ObjectMapper();
       objectMapper.registerModule(new JavaTimeModule());

       User user = objectMapper.readValue(userFromMinIo, User.class);
       return user;
    }

    //update api
    @GetMapping("/test3")
    public List<User> testApi3 () throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException, NoSuchAlgorithmException, ServerException, IllegalArgumentException, IOException, InvalidBucketNameException, NoResponseException, InvalidArgumentException, XmlPullParserException, InvalidPortException, InvalidEndpointException{
        User newUser =new User();
       newUser.setUserId(1212);
       newUser.setEmail("newEmail");
       newUser.setFirstName("newFirstName");
       newUser.setLastName("NewlastName");
       newUser.setPassword("newpassword");
       
       InputStream userFromMinIo = minIoService.updateObject("noonetestbucket2", "testabcd.json",newUser);
       ObjectMapper objectMapper = new ObjectMapper();
       objectMapper.registerModule(new JavaTimeModule());

       List<User> userList = objectMapper.readValue(userFromMinIo, new TypeReference<List<User>>(){});
       return userList;
    }


    //getOne user by Id
    @GetMapping("/test4")
    public User testApi4 () throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException, NoSuchAlgorithmException, ServerException, IllegalArgumentException, IOException, InvalidBucketNameException, NoResponseException, InvalidArgumentException, XmlPullParserException, InvalidPortException, InvalidEndpointException{
       InputStream userFromMinIo = minIoService.getObject("noonetestbucket2", "arrayUsers.json");
       ObjectMapper objectMapper = new ObjectMapper();
       objectMapper.registerModule(new JavaTimeModule());

       List<User> userList = objectMapper.readValue(userFromMinIo, new TypeReference<List<User>>(){});
       int userId = 123;
       for(int i = 0;i < userList.size();i++){
           userList.get(i).getUserId().equals(userId);
           return userList.get(i);
       }
       return null;
    }


    public static String getTOTPCode(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        String hexKey = Hex.encodeHexString(bytes);
        return TOTP.getOTP(hexKey);
    }
    public static String generateSecretKey(String password) {

        //generate secretKey randomly

        // SecureRandom random = new SecureRandom();
        // byte[] bytes = new byte[20];
        // random.nextBytes(bytes);

        Base32 base32 = new Base32();
        return base32.encodeToString(password.getBytes());
    }

    public static String getGoogleAuthenticatorBarCode(String secretKey, String account, String issuer) {
        try {
            return "otpauth://totp/"
                    + URLEncoder.encode(issuer + ":" + account, "UTF-8").replace("+", "%20")
                    + "?secret=" + URLEncoder.encode(secretKey, "UTF-8").replace("+", "%20")
                    + "&issuer=" + URLEncoder.encode(issuer, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void createQRCode(String barCodeData, String filePath, int height, int width)
        throws WriterException, IOException {
    BitMatrix matrix = new MultiFormatWriter().encode(barCodeData, BarcodeFormat.QR_CODE,
            width, height);
    try (FileOutputStream out = new FileOutputStream(filePath)) {
        MatrixToImageWriter.writeToStream(matrix, "png", out);
    }
}

protected List<String> getSaltString() {
    String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    StringBuilder salt = new StringBuilder();
    Random rnd = new Random();
    while (salt.length() < 18) { // length of the random string.
        int index = (int) (rnd.nextFloat() * SALTCHARS.length());
        salt.append(SALTCHARS.charAt(index));
    }
    String saltStr = salt.toString();
    List<String> backupCodes = new ArrayList<>();
    for(int i =0;i<10;i++){
        backupCodes.add(saltStr);
    }
    return backupCodes;
}

}
