import os
import re

def process_scala_file(file_path):
    # 读取文件内容
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 提取所有import语句
    import_pattern = re.compile(r'^\s*import\s+([^/]+?)(?:\s*//.*)?$', re.MULTILINE)
    imports = import_pattern.findall(content)
    
    # 如果没有import语句，跳过处理
    if not imports:
        return
    
    # 处理import内容：去除空格，处理花括号
    processed_imports = set()
    for imp in imports:
        # 去除所有空格
        clean_imp = imp.replace(' ', '')
        
        # 处理花括号中的多个导入
        if '{' in clean_imp and '}' in clean_imp:
            prefix, rest = clean_imp.split('{', 1)
            imports_list = rest.split('}', 1)[0].split(',')
            for item in imports_list:
                if item:  # 跳过空项
                    processed_imports.add(f"import {prefix}{item}")
        else:
            processed_imports.add(f"import {clean_imp}")
    
    # 排序import语句
    sorted_imports = sorted(processed_imports)
    
    # 删除原文件中的所有import语句
    new_content = import_pattern.sub('', content)
    new_content = re.sub(r'\n{3,}', '\n\n', new_content)
    
    # 查找第一个非import语句的位置
    first_non_import = 0
    lines = new_content.splitlines()
    for i, line in enumerate(lines):
        if line.strip() and not line.strip().startswith('package'):
            first_non_import = i
            break
    
    # 在开头插入整理后的import块
    import_block = '\n'.join(sorted_imports)
    lines.insert(first_non_import, f"\n{import_block}\n")
    
    # 重新组合内容
    new_content = '\n'.join(lines)
    
    # 写入文件
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

def main():
    # 遍历当前目录下的所有Scala文件
    # file_path = ".\\server\\UserService\\src\\main\\scala\\Impl\\ModifyUserInfoMessagePlanner.scala"
    # process_scala_file(file_path)
    for root, dirs, files in os.walk('.'):
        for file in files:
            if file.endswith('.scala'):
                file_path = os.path.join(root, file)
                print(f"Processing: {file_path}")
                process_scala_file(file_path)

if __name__ == "__main__":
    main()