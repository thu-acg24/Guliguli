/**
 * QueryUploadVideoPathMessage
 * desc: 根据用户Token校验身份后，通过给定的入参上传视频信息，并生成videoID存储到视频表。
 * @param token: String (用户身份校验的Token)
 * @param videoID: Int (视频ID)
 * @param partCount: Int (视频分片数量)
 * @return uploadPath: UploadPath (上传路径)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryUploadVideoPathMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  partCount: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}
