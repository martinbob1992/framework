package com.marsh.common.starter;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

/**
 * 启动信息类</br>
 * 该类通过实现CommandLineRunner接口，当spring boot启动后会自动运行run方法
 * @author marsh
 * @date 2019-10-15
 */
@Configuration
public class StartupListener implements CommandLineRunner {

	/**
	 * 从配置文件读取项目名称,默认值:Unknown Project
	 */
	@Value("${spring.application.name:Unknown Project}")
	private String projectName;
	
	/**
	 * 读取当前项目使用的端口号,默认值：80
	 */
	@Value("${server.port:80}")
	private Integer port;

	/**
	 * 读取项目访问前缀，默认值：空字符串
	 * 该方式仅适用于spring boot 1这个版本
	 */
	@Value("${server.context-path:}")
	private String contextPath1;

	/**
	 * 读取项目访问前缀，默认值：空字符串
	 * 该方式仅适用于spring boot 2这个版本
	 */
	@Value("${server.servlet.context-path:}")
	private String contextPath2;

	@Value("${spring.profiles.active:}")
	private String active;
	
	/**
	 * 打印一个佛祖保佑和项目启动基本信息
	 */
	@Override
	public void run(String... args) throws Exception {
		String fozuStr = "ICAgICAgICAgICAgICAgICAgIF9vb09vb18KICAgICAgICAgICAgICAgICAgbzg4ODg4ODhvCiAgICAgICAgICAgICAgICAgIDg4IiAuICI4OAogICAgICAgICAgICAgICAgICAofCAtXy0gfCkKICAgICAgICAgICAgICAgICAgT1wgID0gIC9PCiAgICAgICAgICAgICAgIF9fX18vYC0tLSdcX19fXwogICAgICAgICAgICAgLicgIFxcfCAgICAgfC8vICBgLgogICAgICAgICAgICAvICBcXHx8fCAgOiAgfHx8Ly8gIFwKICAgICAgICAgICAvICBffHx8fHwgLTotIHx8fHx8LSAgXAogICAgICAgICAgIHwgICB8IFxcXCAgLSAgLy8vIHwgICB8CiAgICAgICAgICAgfCBcX3wgICcnXC0tLS8nJyAgfCAgIHwKICAgICAgICAgICBcICAuLVxfXyAgYC1gICBfX18vLS4gLwogICAgICAgICBfX19gLiAuJyAgLy0tLi0tXCAgYC4gLiBfXwogICAgICAuIiIgJzwgIGAuX19fXF88fD5fL19fXy4nICA+JyIiLgogICAgIHwgfCA6ICBgLSBcYC47YFwgXyAvYDsuYC8gLSBgIDogfCB8CiAgICAgXCAgXCBgLS4gICBcXyBfX1wgL19fIF8vICAgLi1gIC8gIC8KPT09PT09YC0uX19fX2AtLl9fX1xfX19fXy9fX18uLWBfX19fLi0nPT09PT09CiAgICAgICAgICAgICAgICAgICBgPS0tLT0nCl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXgogICAgICAgICAgICAgICAgIOS9m+elluS/neS9kSAgICAgICDmsLjml6BCVUc=";
		byte[] decode = Base64.getDecoder().decode(fozuStr.getBytes());;
		System.out.println("\n"+new String(decode,"utf-8"));
		String runInfo = "\n---------------------------------------------------------------------\n" +
				"\t %s running!\n" +
				"\t Local:\t\t http://localhost:%s\n" +
				"\t External:\t http://%s:%s\n" +
				"\t Active:\t %s\n" +
				"---------------------------------------------------------------------\n";
		String contextPath = StrUtil.isNotBlank(contextPath1)?contextPath1:contextPath2;
		System.out.println(String.format(runInfo,projectName,port+contextPath,NetUtil.getLocalhost().getHostAddress(),port+contextPath,active));
	}
}
