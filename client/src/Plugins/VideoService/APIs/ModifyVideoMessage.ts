/**
 * ModifyVideoMessage
 * desc: 根据用户Token校验权限后，修改视频表中指定字段的值。
 * @param token: String (用户认证Token，用于校验身份)
 * @param videoID: Int (目标视频的唯一标识ID)
 * @param videoPath: String (视频存储的路径（可选）)
 * @param title: String (视频标题（可选）)
 * @param coverPath: String (视频封面的路径（可选）)
 * @param description: String (视频描述（可选）)
 * @param tag: String (视频标签列表（可选）)
 * @param duration: Int (视频时长（可选）)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class ModifyVideoMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  videoPath: string | null,
        public  title: string | null,
        public  coverPath: string | null,
        public  description: string | null,
        public  tag: string[],
        public  duration: number | null
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}

