/**
 * ValidateVideoMessage
 * desc: 通知服务器视频或封面已经上传完毕
 * @param sessionToken: String (会话Token)
 * @param etags: List[String] (上传分片的ETag列表)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class ValidateVideoMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string,
        public  etags: string[]
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}
