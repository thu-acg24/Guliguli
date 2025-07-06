import json
import requests
import hashlib
import argparse
import sys

def questPost(data: dict, api_name: str, target_ip: str):
    data["serviceName"] = "Tong-Wen"
    data["message_type"] = api_name
    body_str = json.dumps(data, separators=(',', ':'))
    x_hash = hashlib.md5(body_str.encode('utf-8')).hexdigest()
    headers = { "Content-Type": "application/json","X-Hash": x_hash}
    callback_api = f"http://{target_ip}/api/{api_name}"
    response = requests.post(url=callback_api,headers=headers,data=body_str,timeout=10)
    print(response)

def main():
    parser = argparse.ArgumentParser(description='Send POST request to specified API')
    parser.add_argument('--data', type=str, required=True, 
                        help='JSON data string, e.g. \'{"token":"", "targetID": ""}\'')
    parser.add_argument('--api_name', type=str, required=True, 
                        help='API endpoint name')
    parser.add_argument('--target_ip', type=str, required=True,
                        help='Target IP with port, e.g. 10.11.14.1:10012')
    
    args = parser.parse_args()
    
    try:
        # 将JSON字符串转换为字典
        data_dict = json.loads(args.data)
    except json.JSONDecodeError as e:
        print(f"Invalid JSON format in --data: {str(e)}")
        sys.exit(1)
    
    questPost(data_dict, args.api_name, args.target_ip)

if __name__ == "__main__":
    main()