/**
 * QueryUserInContactMessage
 * desc: 根据用户Token验证身份后，查询所有与当前用户有私信记录（发送或接收）的联系人列表。
 * @param token: String (用户的身份验证令牌。)
 * @return contacts: UserInfoWithMessage[] (与当前用户有私信联系的用户信息列表，包括基本信息如昵称与头像路径。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryUserInContactMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10013"
    }
}

