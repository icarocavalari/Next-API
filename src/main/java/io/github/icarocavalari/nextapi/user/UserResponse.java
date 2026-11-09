package io.github.icarocavalari.nextapi.user;

public record UserResponse(Long id, String email, String nickname, Integer age) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname(), user.getAge());
    }
}
