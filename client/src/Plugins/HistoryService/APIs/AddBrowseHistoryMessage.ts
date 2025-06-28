/**
 * AddBrowseHistoryMessage
 * desc: 根据用户Token校验后，记录用户浏览的视频到历史记录表。
 * @param token: String (用户的身份验证信息，用于校验用户身份是否合法。)
 * @param videoID: Int (视频的唯一标识符，用于指定用户浏览的视频。)
 * @return result: String (操作结果的描述，记录是否成功或失败的信息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class AddBrowseHistoryMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10017"
    }
}

