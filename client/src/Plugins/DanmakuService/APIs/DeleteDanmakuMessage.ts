/**
 * DeleteDanmakuMessage
 * desc: 用于删除弹幕功能点
 * @param token: String (用户的令牌，用于校验用户身份)
 * @param danmakuID: Int (弹幕的唯一标识ID)
 * @return result: String (操作结果，返回None表示成功，或者具体的错误信息)
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
        return "127.0.0.1:10014"
    }
}

