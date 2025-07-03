/**
 * ModifyVideoMessage
 * desc: 根据用户Token校验权限后，修改视频表中指定字段的值。
 * @param token: String (用户认证Token，用于校验身份)
 * @param videoID: Int (目标视频的唯一标识ID)
 * @param title: String (视频标题（可选）)
 * @param description: String (视频描述（可选）)
 * @param tag: String[] (视频标签列表（可选）)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class ModifyVideoMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  title: string | null,
        public  description: string | null,
        public  tag: string[] | null,
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}

