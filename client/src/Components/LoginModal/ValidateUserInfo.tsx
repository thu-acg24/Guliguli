export const validateUsername = (username: string): string => {
    const trimmedUsername = username.trim();
    if (!trimmedUsername) {
        throw "请输入用户名";
    }
    if (trimmedUsername.length < 3 || trimmedUsername.length > 20) {
        throw "用户名长度需为3-20个字符";
    }
    return trimmedUsername;
}

export const validateEmail = (email: string): void => {
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
        throw "请输入邮箱";
    }
    const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    if (!emailRegex.test(trimmedEmail)) {
        throw "请输入有效的邮箱地址";
    }
};

export const validatePassword = (password: string, confirmPassword: string): void => {
    if (password !== confirmPassword) {
        throw "两次输入的密码不一致";
    }
    if (!password) {
        throw "请输入密码";
    }
    if (password.length < 6 || password.length > 20) {
        throw "密码长度需为6-20个字符";
    }
}