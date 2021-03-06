package com.example.yhwasongtest.user.controller;

import com.example.yhwasongtest.user.dto.UserModelDto;
import com.example.yhwasongtest.user.model.UserModel;
import com.example.yhwasongtest.user.service.UserService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping(value = "/api/v1")
    public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @Autowired
    public UserController( UserService userService) {
        this.userService = userService;

    }
    /**
     * 회원가입
     *
     * @param userModelDto 회원정보가 들어있는 DTO
     * @return 저장되는 회원의 PK
     */
    @ApiOperation(value="회원가입")
    @RequestMapping(method = RequestMethod.POST, value = "/user")
    public ResponseEntity signup(@RequestBody  UserModelDto userModelDto){ // 회원 추가
        try {
            UserModel userModel = userService.signUp(userModelDto);
            return new ResponseEntity(userModel.getUsername(), HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity(e.toString(), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation(value="로그아웃")
    @GetMapping(value = "/logout")
    public ResponseEntity logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            userService.logout(request, response);
            return new ResponseEntity(HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value="로그인")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username",value ="이름" ,required = true , dataType="String", paramType="query"),
            @ApiImplicitParam(name = "password",value ="비밀번호" ,required =true ,  dataType="String", paramType="query")
    })
    @GetMapping(value = "/login")
    public ResponseEntity login(@RequestParam(name = "username", required = true) String username,
                           @RequestParam(name = "password", required = true) String password,
                                HttpServletRequest request,    HttpServletResponse response
                            ) {
        try {
            HttpSession session = request.getSession();
            Object userModel =session.getAttribute("login");
            if(userModel!=null){
                response.sendRedirect(request.getContextPath());
                return new ResponseEntity(userModel, HttpStatus.OK);
            }

            UserModel user = userService.login(username, password, request);

            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", user.getUsername());
            jsonObject.put("role", user.getRole());
            jsonArray.put(jsonObject);

            return  new ResponseEntity<>(jsonObject.toString(), HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(e.toString(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity home(UserModel model)
    {

        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            HttpSession httpSession = request.getSession(false);

            Object user = httpSession.getAttribute("login");
            return  new ResponseEntity<>(user, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(value = "/history")
    public ResponseEntity getLoginHistory()
    {

        try {
            int cnt = userService.getLoginHistory();
            return  new ResponseEntity<>(cnt, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(value = "/sendEmail")
    public ResponseEntity sendGoogleMail(@RequestParam(value = "username", required = true) String username,
                                        @RequestParam(value = "email", required = true) String email) {
        try {


            userService.sendGoogleMail(username, email);
            return  new ResponseEntity<>(null, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(e.toString(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(value = "/changePassword")
    public ResponseEntity changePassword(@RequestBody UserModelDto userModelDto) {
        try {
            userService.changePassword(userModelDto);
            return  new ResponseEntity<>(null, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(e.toString(), HttpStatus.BAD_REQUEST);
        }
    }
}
