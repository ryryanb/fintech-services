package com.ryanbondoc.fintech.auth.service;

import com.ryanbondoc.fintech.auth.dto.RegisterRequest;
import com.ryanbondoc.fintech.auth.dto.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
}