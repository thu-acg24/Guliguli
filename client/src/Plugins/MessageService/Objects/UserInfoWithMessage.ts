/**
 * UserInfoWithMessage
 * desc: 用户信息与最后一条聊天
 * @param userInfo: UserInfo (用户信息)
 * @param unreadCount: Int (未读消息数)
 * @param timestamp: DateTime (最后一条消息发送的时间戳)
 * @param content: String (最后一条消息的内容)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'
import {UserInfo} from "Plugins/UserService/Objects/UserInfo";




export class UserInfoWithMessage extends Serializable {
    constructor(
        public  userInfo: UserInfo,
        public  unreadCount: number,
        public  timestamp: string,
        public  content: string,
    ) {
        super()
    }
}


