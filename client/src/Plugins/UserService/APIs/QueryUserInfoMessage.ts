/**
 * QueryUserInfoMessage
 * desc: 根据用户ID查询用户表，返回用户的基本信息。
 * @param userID: Int (用户的唯一标识)
 * @return user: UserInfo (查询返回的用户基本信息，包括ID、用户名、头像路径和封禁状态)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryUserInfoMessage extends TongWenMessage {
    constructor(
        public  userID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

