package com.pawankundar.expensetracker.services;

import com.pawankundar.expensetracker.domain.User;
import com.pawankundar.expensetracker.exceptions.EtAuthException;

public interface UserService {
    User validateUser(String email,String password) throws EtAuthException;
    User registerUser(String firstName,String lastName,String email,String password) throws EtAuthException;
}
