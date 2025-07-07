/**
 * ValidateCoverMessage
 * desc: 通知服务器视频或封面已经上传完毕
 * @param sessionToken: String (会话Token)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class ValidateCoverMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}
