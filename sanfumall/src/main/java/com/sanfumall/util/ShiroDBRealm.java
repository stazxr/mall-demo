package com.sanfumall.util;

import javax.annotation.Resource;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import com.sanfumall.common.pojo.entity.Member;
import com.sanfumall.common.util.ConstantUtil;
import com.sanfumall.common.util.security.MallToken;
import com.sanfumall.service.MemberService;
import com.sanfumall.common.util.security.MD5Util;

/**
 * 创建shiroDBRealm自定义授权与认证功能
 * extends AuthorizingRealm， 该类通过增加了授权（Authorization）的功能
 * 扩展了AuthenticatingRealm类
 * @author SunTao
 * @since 2018-12-13
 */
public class ShiroDBRealm extends AuthorizingRealm {
	
	@Resource(name="memberService")
	private MemberService memberService;
	
	// shiro授权功能，功能预留
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection arg0) {
		return null; 
	}

	// shiro认证功能,验证码认证成功后，通过Subject.login(token)进入该方法,进行其他登录信息的认证
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		// 将AuthenticationToken强制转换为BabyMallToken
		MallToken babyMalltoken = (MallToken) token;
		// 获取用户登录时的登录名和登录密码
		String email = babyMalltoken.getUsername();
		char[] pwd = babyMalltoken.getPassword();
		try {
			// 1、校验用户是否填写登录名和登录密码
			if (email != null && !"".equals(email.trim()) && pwd != null && pwd.length > 0) {
				// 将userToken中的密码数组转换为字符串，并进行加密,随后将加密后的密码绑定到token中
				String password = MD5Util.encrypt(new String(pwd));
				babyMalltoken.setPassword(password.toCharArray());
				// 根据昵称查找会员信息
				Member member = memberService.getMemberByEmail(email.trim());
				// 2、校验登录名是否存在
				if (member != null) {
					// 3、校验登录密码是否正确
					if (password.equals(member.getPassword())) {
						// 4、判断欲登录用户是否处于启用状态
						if (ConstantUtil.STATUS_ENABLE.equals(member.getStatus().getStatusCode())) {
							SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(member.getNickname(), member.getPassword(), getName());
							// 默认登陆成功，绑定session对象
							SecurityUtils.getSubject().getSession().setAttribute("member", member);
							return info;
						} else {
							throw new DisabledAccountException("the member is not allow login!please add qq:1027353579 for more information!");
						}
					} else {
						throw new UnknownAccountException("password is not correct!");
					}
				} else {
					throw new UnknownAccountException("email is not exist!");
				}
			} else {
				throw new UnknownAccountException("email or password must not be null!");
			}
		} catch (UnknownAccountException e) {
			e.printStackTrace();
			return null;
		} catch (DisabledAccountException e) {
			throw new AuthenticationException(e.getMessage());
		} catch (Exception e) {
			throw new AuthenticationException("System error!please call admin! +q:1027353579");
		}
	}
	
}