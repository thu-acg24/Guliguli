/**
 * ValidateAvatarMessage
 * desc: 通知服务器上传头像完毕
 * @param sessionToken: String (上传会话Token)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class ValidateAvatarMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

