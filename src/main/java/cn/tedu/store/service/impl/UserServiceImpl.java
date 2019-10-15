package cn.tedu.store.service.impl;

import java.util.Date;
import java.util.UUID;

import cn.tedu.store.service.exception.PasswordNotMatchException;
import cn.tedu.store.service.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import cn.tedu.store.entity.User;
import cn.tedu.store.mapper.UserMapper;
import cn.tedu.store.service.IUserService;
import cn.tedu.store.service.exception.InsertException;
import cn.tedu.store.service.exception.UsernameDuplicateException;

/**
 * 处理用户相关功能的业务层实现类
 */
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public void reg(User user) {
        // 从参数user中获取username
        String username = user.getUsername();
        // 根据username查询用户数据：User result = userMapper.findByUsername(username)
        User result = userMapper.findByUsername(username);
        // 判断查询结果是否不为null
        if (result != null) {
            // 抛出异常：throw new UsernameDuplicateException();
            throw new UsernameDuplicateException("该用户名已被使用！");
        }

        // 补全数据-加密后的密码、盐值
        // 1. 生成盐值
        String salt = UUID.randomUUID().toString().toUpperCase();
        // 2. 取出用户提交的原始密码
        String password = user.getPassword();
        // 3. 执行加密
        String md5Password = getMd5Password(password, salt);
        // 4. 将盐值和加密后的密码补全到user对象中
        user.setSalt(salt);
        user.setPassword(md5Password);

        // 补全数据-isDelete：0
        user.setIsDelete(0);
        // 补全数据-4项日志
        Date date = new Date();
        user.setCreatedUser(username);
        user.setCreatedTime(date);
        user.setModifiedUser(username);
        user.setModifiedTime(date);

        // 执行插入用户数据，获取返回值：Integer rows = userMapper.insert(user)
        Integer rows = userMapper.insert(user);
        // 判断返回的受影响行数是否不为1
        if (rows != 1) {
            // 抛出异常：throw new InsertException();
            throw new InsertException("插入用户数据异常！请联系系统管理员！");
        }
    }

    /**
     * 处理用户登录业务
     *
     * @param username 用户名
     * @param password 用户密码
     * @return user对象
     */
    public User login(String username, String password) {
        // 根据参数username查询用户数据
        User result = userMapper.findByUsername(username);
        // 判断查询结果是否为null
        if (result != null) {
            // 判断查询结果中的isDelete是否为1,
            if (result.getIsDelete() != 1) {
                // 从查询结果中获取盐值
                String salt = result.getSalt();
                // 根据参数password和盐值得到加密后的密码(md5Password)
                String md5Password = getMd5Password(password, salt);
                // 判断查询结果中的密码与md5Password是否不一致
                // 是：密码错误，抛出PasswordNotMatchException
                if (!md5Password.equals(result.getPassword())) {
                    throw new PasswordNotMatchException("密码不匹配");
                }
            }
            //是：用户数据被标记为“已删除”，抛出UserNotFoundException
            else {
                throw new UserNotFoundException("该用户不存在！");
            }
        } else {
            // 是：没有与username匹配的数据，用户名不存在，抛出UserNotFoundException，并描述错误
            throw new UserNotFoundException("该用户不存在！");
        }
        // 创建新的User对象
        User user = new User();
        // 将查询结果中的uid、username、avatar封装到新对象中
        user.setUid(result.getUid());
        user.setUsername(result.getUsername());
        user.setAvatar(result.getAvatar());
        // 将新User对象返回
        return user;
    }

    /**
     * 执行加密
     *
     * @param password 原始密码
     * @param salt     盐值
     * @return 加密后的结果
     */
    private String getMd5Password(String password, String salt) {
        // 加密标准：将盐拼在原始密码的左右两侧，循环加密3次
        for (int i = 0; i < 3; i++) {
            password = DigestUtils.md5DigestAsHex(
                    (salt + password + salt).getBytes()).toUpperCase();
        }
        return password;
    }


}