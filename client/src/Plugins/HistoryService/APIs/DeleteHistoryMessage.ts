/**
 * DeleteHistoryMessage
 * desc: 根据用户Token校验后，从历史记录表删除指定记录。
 * @param token: String (用户的身份令牌，用于校验用户是否合法。)
 * @param videoID: Int (需要删除的历史记录中对应的视频ID。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class DeleteHistoryMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["History"]
    }
}

