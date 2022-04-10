package com.pawankundar.expensetracker.services;

import java.util.regex.Pattern;

import com.pawankundar.expensetracker.domain.User;
import com.pawankundar.expensetracker.exceptions.EtAuthException;
import com.pawankundar.expensetracker.repositories.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServieImpl implements UserService{

    @Autowired
    UserRepository userRepository;

    @Override
    public User validateUser(String email, String password) throws EtAuthException {
        Pattern pattern = Pattern.compile("^(.+)@(.+)$");
        if (email!= null) email = email.toLowerCase();
        if(!pattern.matcher(email).matches()){
            throw new EtAuthException("Invalid Email Format");
        }
        return userRepository.findByEmailAndPassword(email, password);
    }

    @Override
    public User registerUser(String firstName, String lastName, String email, String password) throws EtAuthException {
        Pattern pattern = Pattern.compile("^(.+)@(.+)$");
        if (email!= null) email = email.toLowerCase();
        if(!pattern.matcher(email).matches()){
            throw new EtAuthException("Invalid Email Format");
        }

        Integer count = userRepository.getCountByEmail(email);
        if(count > 0){
        throw new EtAuthException("Email Already exists");
        }

        Integer userId = userRepository.create(firstName, lastName, email, password);

        return userRepository.findById(userId);

    }
    
}
