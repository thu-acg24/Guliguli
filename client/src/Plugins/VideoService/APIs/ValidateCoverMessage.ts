/**
 * ValidateCoverMessage
 * desc: 通知服务器视频或封面已经上传完毕
 * @param sessionToken: String (会话Token)
 * @param isVideo: Boolean (是否为视频)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class ValidateCoverMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string,
        public  isVideo: boolean
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}
