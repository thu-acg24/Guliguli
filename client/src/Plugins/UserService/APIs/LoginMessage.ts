/**
 * LoginMessage
 * desc: 用户传入昵称或邮箱和密码，校验登录信息，如果通过，则生成Token并返回。
 * @param usernameOrEmail: String (登录用户名或邮箱地址，用于进行身份查询。)
 * @param password: String (登录用户的明文密码，用于验证身份。)
 * @return tokenAndResult: String[] (登录结果，返回由Token(空字符串表示失败)和结果信息(如成功或具体错误)组成的列表。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class LoginMessage extends TongWenMessage {
    constructor(
        public  usernameOrEmail: string,
        public  password: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}

