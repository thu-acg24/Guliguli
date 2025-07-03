/**
 * RegisterMessage
 * desc: 用户传入昵称、邮箱以及密码，计算密码哈希，生成userID并存储到用户表，同时初始化用户角色为普通用户，用于用户注册功能点
 * @param username: String (用户的用户名，用于注册身份标识)
 * @param email: String (用户的邮箱，用于注册身份标识)
 * @param password: String (明文密码，用于注册时计算用户的密码哈希值)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class RegisterMessage extends TongWenMessage {
    constructor(
        public  username: string,
        public  email: string,
        public  password: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

