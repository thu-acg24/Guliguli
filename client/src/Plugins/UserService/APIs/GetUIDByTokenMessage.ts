/**
 * GetUIDByTokenMessage
 * desc: 根据用户Token，返回该Token对应的用户ID，Token不合法则返回None。
 * @param token: String (用户的会话Token，用于校验身份。)
 * @return userID: Int (Token解析后的用户ID。若Token无效则返回None。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class GetUIDByTokenMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

