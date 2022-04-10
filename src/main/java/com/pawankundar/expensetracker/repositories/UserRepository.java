package com.pawankundar.expensetracker.repositories;

import com.pawankundar.expensetracker.domain.User;
import com.pawankundar.expensetracker.exceptions.EtAuthException;

public interface UserRepository {

    Integer create(String firstName, String lastName, String email, String password) throws EtAuthException;

    User findByEmailAndPassword(String email, String password) throws EtAuthException;

    Integer getCountByEmail(String email) throws EtAuthException;

    User findById(Integer userId) throws EtAuthException;

}
