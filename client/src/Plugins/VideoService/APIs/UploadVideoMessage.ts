/**
 * UploadVideoMessage
 * desc: 根据用户Token校验身份后，通过给定的入参上传视频信息，并生成videoID存储到视频表。
 * @param token: String (用户身份校验的Token)
 * @param title: String (视频标题)
 * @param description: String (视频简介)
 * @param tag: String (视频标签列表)
 * @return videoID: Int (生成的视频ID)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class UploadVideoMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  title: string,
        public  description: string,
        public  tag: string[],
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}

