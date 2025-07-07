/**
 * UploadPath
 * desc: 上传文件的相关信息
 * @param path: List[String] (上传文件的minio自签名链接)
 * @param token: String (上传会话Token)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class UploadPath extends Serializable {
    constructor(
        public  path: string[],
        public  token: string,
    ) {
        super()
    }
}


