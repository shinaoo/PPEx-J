关于工程说明:

结构说明:
    首先里面有client和server包,这里就区分client和server实现
    如果不是在client和server包里的,则是client和server共同有的.
    
Message说明:
    +----------------------------------------------------------------------------------------------+
    -    Message(8位version,32位contentlength,content)                                              -
    -     +----------------------------------------------------------------------------------------+
    -     -    content (暂定json字符串,解析成ContentMsg)                                              - 
    -     -     +-----------------------------------------------------------------------------------+
    -     -     -     body(暂定json字符串,解析成各种ppex.proto.type包下的各个TypeMessage)               -
    -     -     +-----------------------------------------------------------------------------------+
    -     +-----------------------------------------------------------------------------------------+
    +-----------------------------------------------------------------------------------------------+
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    