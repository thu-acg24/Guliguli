/**
 * QueryNoticesCountMessage
 * desc: 根据用户Token验证身份后，查询当前用户未读信息数。
 * @param token: String (用户的身份令牌，用于验证用户身份。)
 * @return messages: NoticesCount (未读信息数)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryNoticesCountMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Message"]
    }
}

