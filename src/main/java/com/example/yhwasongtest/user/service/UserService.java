package com.example.yhwasongtest.user.service;

import com.example.yhwasongtest.common.JwtUtil;
import com.example.yhwasongtest.user.dto.UserModelDto;
import com.example.yhwasongtest.user.model.Authority;
import com.example.yhwasongtest.user.model.CustomUserDetails;
import com.example.yhwasongtest.user.model.LoginHistory;
import com.example.yhwasongtest.user.model.UserModel;
import com.example.yhwasongtest.user.repository.AuthorityRepository;
import com.example.yhwasongtest.user.repository.LoginHistoryRepository;
import com.example.yhwasongtest.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class UserService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    public UserService(UserRepository userRepository,
                       AuthorityRepository authorityRepository,
                       LoginHistoryRepository loginHistoryRepository) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserModel user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException(username + "is not found.");
        }

        CustomUserDetails quickGuideUser = new CustomUserDetails();
        quickGuideUser.setUsername(user.getUsername());
        quickGuideUser.setPassword(user.getPassword());
        quickGuideUser.setAuthorities(getAuthorities(username));
        quickGuideUser.setEnabled(true);
        quickGuideUser.setAccountNonExpired(true);
        quickGuideUser.setAccountNonLocked(true);
        quickGuideUser.setCredentialsNonExpired(true);

        return quickGuideUser;
    }

    public Collection<GrantedAuthority> getAuthorities(String username) {
        List<Authority> authList = authorityRepository.findByUsername(username);
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Authority authority : authList) {
            authorities.add(new SimpleGrantedAuthority(authority.getAuthority()));
        }
        return authorities;
    }


    /**
     * 회원정보 저장
     *
     * @param infoDto 회원정보가 들어있는 DTO
     * @return 저장되는 회원의 PK
     */
    public Long save(UserModelDto infoDto) throws Exception {
        //BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        //infoDto.setPassword(encoder.encode(infoDto.getPassword()));
        try {
            UserModel userModel = insertUser(infoDto);

            userRepository.save(userModel);
            return userModel.getId();
        } catch (Exception e) {
            throw new Exception();
        }
    }

    public UserModel insertUser(UserModelDto userModelDto) throws Exception {
        Optional<UserModel> userModelOptional = userRepository.findAllByUsername(userModelDto.getEmail());

        if (!userModelOptional.isPresent()) {
            try {

                String resultToken = getToken(userModelDto.getEmail(), userModelDto.getPassword());
                resultToken = getHashed(resultToken);

                UserModel userModel = new UserModel(userModelDto.getEmail(), resultToken, "ROLE_USER");

                return userModel;
            } catch (Exception e) {
                logger.info("Exception ===>   ", e);
            }
            ;
        }
        UserModel userModel = userRepository.findByUsername(userModelDto.getEmail());
        return userModel;
    }

    public UserModel getUser(String userName){
        return userRepository.findByUsername(userName);
    }

    public String getToken(String id, String password) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> object = new HashMap<String, String>();
        object.put("typ", "JWT");
        object.put("alg", "HS256");
        String bytes = mapper.writeValueAsString(object);
        String headerResult = Base64.getUrlEncoder().encodeToString(bytes.getBytes());
        headerResult = headerResult.replaceAll("=", "");

        Map<String, String> object1 = new HashMap<String, String>();
        object1.put("iss", "mapyhwasong.com");
        object1.put("exp", "1485270000000");
        object1.put("https://github.com/songyunhwa/springBootProject_back", "true");
        object1.put("userId", id);
        object1.put("password", password);
        String bytes1 = mapper.writeValueAsString(object1);
        String bodyResult = Base64.getUrlEncoder().encodeToString(bytes1.getBytes());
        bodyResult = bodyResult.replaceAll("=", "");

        return headerResult + "." + bodyResult;
    }

    public String getHashed(String password) {
        String passwordHashed = BCrypt.hashpw(password, BCrypt.gensalt());
        return passwordHashed;
    }

    public ResponseEntity login(String name, String password, HttpServletRequest request, HttpSession httpSession) throws Exception {


        UserModel userModel = userRepository.findByUsername(name);
        String ip = this.getRemoteAddr(request);

        // session 기간이 아직 지나지 않았다면
        if(userModel.getDate()!=null && userModel.getDate().compareTo(new Date()) > 0){
            // 기간을 다시 설정
            Date date = new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24 * 7));
            this.keepLogin(userModel.getUsername(), userModel.getSessionId(), date);

            this.putHistory(userModel.getUsername(), ip);

            return new ResponseEntity(userModel, HttpStatus.OK);
        }

        HttpSession session = request.getSession(true);

        if (userModel != null) {
            String resultToken = getToken(name, password);
            //if (userModel.getPassword().equals(resultToken)) {
            if (BCrypt.checkpw(resultToken, userModel.getPassword())) {

                // 세션 설정
                session.setAttribute("login", userModel);
                session.setMaxInactiveInterval(1800); //30분

                // 세션 유효시간 설정
                Date date = new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24 * 7));
                this.keepLogin(userModel.getUsername(), session.getId(), date);

                this.putHistory(userModel.getUsername(), ip);
                /*
                // 토큰 생성
                String accessToken = JwtUtil.createToken(userModel.getId(), userModel.getUsername());
                String url ="/session";
                return ResponseEntity.created(new URI(url)).body(SessionResponseDto
                        .builder()
                        .email(userModel.getUsername())
                        .token(accessToken)
                        .build()); */
            }
            return new ResponseEntity(userModel, HttpStatus.OK);
        }

        return new ResponseEntity(userModel, HttpStatus.BAD_REQUEST);
    }

    public void logout(HttpSession httpSession) {
        httpSession.invalidate();
        /*
        Cookie loginCookie = WebUtils.getCookie(request, "loginUser");
        if (loginCookie != null) {
            loginCookie.setPath("/");
            loginCookie.setMaxAge(0);
            response.addCookie(loginCookie);

            // 사용자 테이블에서도 유효기간을 현재시간으로 다시 세팅해줘야함.
            Date date = new Date(System.currentTimeMillis());
            this.keepLogin(loginCookie.getValue(), null, date);
        }
        */
    }

    public void keepLogin(String username, String sessionId, Date date) {

        UserModel userModel = userRepository.findByUsername(username);
        if (userModel != null) {
            userModel.setSessionId(sessionId);
            userModel.setDate(date);
            userRepository.save(userModel);
        }
    }

    public void putHistory(String userName, String ip) throws ParseException {
        boolean exist = false;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date currentDate =new Date();
        String date = format.format(currentDate);

        String start = date + " 00:00:00";
        String end = date + " 23:59:59";

        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 유저가 오늘 접속했는지 // 같은 ip를 갖고 있는지 확인
        List<LoginHistory> loginHistories= loginHistoryRepository.findByUserNameAndLoginDateBetween(userName, format.parse(start), format.parse(end));
        for(LoginHistory history : loginHistories) {
            if(history.getIp().equals(ip)){
                exist = true;
                break;
            }
        }
        if(!exist){
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setIp(ip);
            loginHistory.setUserName(userName);
            loginHistory.setLoginDate(new Date());
            loginHistoryRepository.save(loginHistory);
        }

    }

    public String getRemoteAddr(HttpServletRequest request) {

        String ip = null;

        ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;

    }
    public int getLoginHistory() throws ParseException{
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date currentDate =new Date();
        String date = format.format(currentDate);

        String start = date + " 00:00:00";
        String end = date + " 23:59:59";

        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        List<LoginHistory> loginHistories= loginHistoryRepository.findByLoginDateBetween(format.parse(start), format.parse(end));
        return loginHistories.size();
    }

}