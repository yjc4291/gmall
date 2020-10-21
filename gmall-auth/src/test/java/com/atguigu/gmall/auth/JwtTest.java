package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "D:\\workspace\\idea_workspace\\gmall\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\workspace\\idea_workspace\\gmall\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 1);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE1OTk0NzkwNjh9.UoaTXY7iTJu7B5Bzpj2Qu919eX0N42eh6-r8ZfUM_6xNGs67O3Y-5s4itixsTJPZa0k4TO_OW1Sgamcf3UQS3jTxdOYHhaBp567UedQPCJPsgMbT5KgB3pAb-cYYN5xmSAEEcl3YongjWWaKgzEeZyjLL14x2nW2Jl51cFgK7DwUpjx7de3d8lvr-svr_pb6Wqsot7i_Ckcnb61i_xsO1UUJWiJznG0KJmbWTRRK_R6wTouQR4bS8t66Nkc2k2Tt-uuEg_6xPoR3p9k215fbTo6B9sKZO--B7UPdZf6hFWuYGKh2XiiSRKjHDJGXfRz0MIQRpkIWHoMszsj7BR6zaQ";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}