/**
 * DeleteDanmakuMessage
 * desc: 用于删除弹幕功能点
 * @param token: String (用户的令牌，用于校验用户身份)
 * @param danmakuID: Int (弹幕的唯一标识ID)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class DeleteDanmakuMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  danmakuID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Danmaku"]
    }
}

