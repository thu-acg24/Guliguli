/**
 * LoginMessage
 * desc: 用户传入昵称或邮箱和密码，校验登录信息，如果通过，则生成Token并返回。
 * @param usernameOrEmail: String (登录用户名或邮箱地址，用于进行身份查询。)
 * @param password: String (登录用户的明文密码，用于验证身份。)
 * @return token: String (用户Token)
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
        return ServerAddr["User"]
    }
}

