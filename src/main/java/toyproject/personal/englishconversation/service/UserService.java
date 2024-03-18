package toyproject.personal.englishconversation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import toyproject.personal.englishconversation.config.jwt.JwtProvider;
import toyproject.personal.englishconversation.controller.dto.user.SignUpRequestDto;
import toyproject.personal.englishconversation.controller.dto.user.SignInRequestDto;
import toyproject.personal.englishconversation.controller.dto.user.SignInResultDto;
import toyproject.personal.englishconversation.domain.RefreshToken;
import toyproject.personal.englishconversation.domain.User;
import toyproject.personal.englishconversation.exception.UserEmailException;
import toyproject.personal.englishconversation.exception.UserPasswordException;
import toyproject.personal.englishconversation.mapper.RefreshTokenMapper;
import toyproject.personal.englishconversation.mapper.UserMapper;

import java.time.Duration;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenMapper refreshTokenMapper;

    public void save(SignUpRequestDto signUpRequestDto) {
        userMapper.save(User.builder()
                        .email(signUpRequestDto.getEmail())
                        .password(passwordEncoder.encode(signUpRequestDto.getPassword())) // 암호화한 비밀번호 저장
                        .nickname(signUpRequestDto.getNickname())
                        .build());
    }

    public SignInResultDto login(SignInRequestDto signInRequestDto){
        User user = userMapper.findByEmail(signInRequestDto.getEmail());
        if(user == null){
            throw new UserEmailException("아이디 불일치");
        }
        /*
        1. user.password에 저장된 해시값에서 salt값을 추출해서 SignInRequestDto의 비밀번호를 해싱 (salt: 해시값의 일부)
        2. user.password에 저장된 해시값에서 비밀번호 부분을 추출 (비밀번호: 해시값의 일부)
        3. user.password에서 추출한 비밀번호와 해싱된 signInRequestDto의 비밀번호가 일치하는지 확인
        */
        if(!passwordEncoder.matches(signInRequestDto.getPassword(), user.getPassword())){
            throw new UserPasswordException("비밀번호 불일치");
        }

        String newAccessToken = jwtProvider.generateToken(user, Duration.ofMinutes(30), List.of("ROLE_USER"));
        String newRefreshToken = jwtProvider.generateToken(user, Duration.ofDays(7), List.of("ROLE_USER"));

        RefreshToken refreshToken = refreshTokenMapper.findByUserId(user.getId());
        if(refreshToken != null) {
            refreshTokenMapper.update(user.getId(), newRefreshToken);
        }
        else {
            refreshTokenMapper.save(user.getId(), newRefreshToken);
        }

        return new SignInResultDto(newAccessToken, newRefreshToken);
    }

    public User getUserById(Long userId) {
        User user = userMapper.findById(userId);
        if(user == null){
            throw new UserEmailException("아이디 불일치");
        }
        return user;
    }
}
