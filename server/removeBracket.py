import os
import re

def replace_in_file(filepath):
    # 正则匹配 ${xxx}，其中xxx只包含字母数字
    pattern = re.compile(r'\$\{([a-zA-Z0-9]+)\}')
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 替换所有匹配项
    new_content = pattern.sub(lambda m: f'${m.group(1)}', content)
    
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Processed: {filepath}")
    else:
        print(f"No changes needed: {filepath}")

def process_directory(directory='.'):
    for root, _, files in os.walk(directory):
        for filename in files:
            if filename.endswith('.scala'):
                filepath = os.path.join(root, filename)
                replace_in_file(filepath)

if __name__ == '__main__':
    # 可以修改为你要处理的目录路径，默认当前目录
    process_directory()