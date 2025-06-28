/**
 * DeleteVideoMessage
 * desc: 根据用户Token校验权限后，根据videoID删除视频记录。
 * @param token: String (用户身份验证Token。)
 * @param videoID: Int (需要删除的视频的唯一标识ID。)
 * @return result: String (操作结果，若失败返回错误信息，可为None。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class DeleteVideoMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10016"
    }
}

