/**
 * QueryMessagesMessage
 * desc: 根据用户Token验证身份后，查询当前用户和目标用户之间的私信记录。
 * @param token: String (用户的身份令牌，用于验证用户身份。)
 * @param targetID: Int (目标用户的用户ID，用于查询与该用户的私信记录。)
 * @return messages: Message[] (私信记录的列表，包含用户之间的消息内容、时间和其他相关字段。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryMessagesMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  targetID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Message"]
    }
}

