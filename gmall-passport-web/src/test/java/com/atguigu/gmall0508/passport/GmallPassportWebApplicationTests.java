package com.atguigu.gmall0508.passport;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.atguigu.gmall0508.passport.config.JwtUtil;
import org.codehaus.groovy.runtime.powerassert.SourceText;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void test0001(){
		String key="songzhengjian";

		Map<String,Object> map=new HashMap();
		map.put("userId","100101");
		map.put("nickName","monkey");

		String salt="192.168.236.130";

		String token = JwtUtil.encode(key, map, salt);
		System.err.println("token="+token);
		Map<String, Object> decode = JwtUtil.decode(token, key, salt);
		System.err.println(decode);


	}

}
