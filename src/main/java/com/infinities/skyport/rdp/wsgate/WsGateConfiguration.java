package com.infinities.skyport.rdp.wsgate;

public class WsGateConfiguration {

	private Global global = new Global();
	private Ssl ssl = new Ssl();
	private Threading threading = new Threading();
	private Http http = new Http();
	private Acl acl = new Acl();
	private Rdpoverride rdpoverride = new Rdpoverride();
	private Openstack openstack = new Openstack();
	private Hyperv hyperv = new Hyperv();


	public Global getGlobal() {
		return global;
	}

	public void setGlobal(Global global) {
		this.global = global;
	}

	public Ssl getSsl() {
		return ssl;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	public Threading getThreading() {
		return threading;
	}

	public void setThreading(Threading threading) {
		this.threading = threading;
	}

	public Http getHttp() {
		return http;
	}

	public void setHttp(Http http) {
		this.http = http;
	}

	public Acl getAcl() {
		return acl;
	}

	public void setAcl(Acl acl) {
		this.acl = acl;
	}

	public Rdpoverride getRdpoverride() {
		return rdpoverride;
	}

	public void setRdpoverride(Rdpoverride rdpoverride) {
		this.rdpoverride = rdpoverride;
	}

	public Openstack getOpenstack() {
		return openstack;
	}

	public void setOpenstack(Openstack openstack) {
		this.openstack = openstack;
	}

	public Hyperv getHyperv() {
		return hyperv;
	}

	public void setHyperv(Hyperv hyperv) {
		this.hyperv = hyperv;
	}


	public class Global {

		private String daemon;
		private String pidfile;
		private String debug;
		private String enablecore;
		private String hostname;
		private Integer port;
		private String bindaddr;
		private String redirect;
		private String logmask;
		private String logfacility;


		public String getDaemon() {
			return daemon;
		}

		public void setDaemon(String daemon) {
			this.daemon = daemon;
		}

		public String getPidfile() {
			return pidfile;
		}

		public void setPidfile(String pidfile) {
			this.pidfile = pidfile;
		}

		public String getDebug() {
			return debug;
		}

		public void setDebug(String debug) {
			this.debug = debug;
		}

		public String getEnablecore() {
			return enablecore;
		}

		public void setEnablecore(String enablecore) {
			this.enablecore = enablecore;
		}

		public String getHostname() {
			return hostname;
		}

		public void setHostname(String hostname) {
			this.hostname = hostname;
		}

		public Integer getPort() {
			return port;
		}

		public void setPort(Integer port) {
			this.port = port;
		}

		public String getBindaddr() {
			return bindaddr;
		}

		public void setBindaddr(String bindaddr) {
			this.bindaddr = bindaddr;
		}

		public String getRedirect() {
			return redirect;
		}

		public void setRedirect(String redirect) {
			this.redirect = redirect;
		}

		public String getLogmask() {
			return logmask;
		}

		public void setLogmask(String logmask) {
			this.logmask = logmask;
		}

		public String getLogfacility() {
			return logfacility;
		}

		public void setLogfacility(String logfacility) {
			this.logfacility = logfacility;
		}

	}

	public class Ssl {

		private Integer port;
		private String bindaddr;
		private String certfile;
		private String certpass;


		public Integer getPort() {
			return port;
		}

		public void setPort(Integer port) {
			this.port = port;
		}

		public String getBindaddr() {
			return bindaddr;
		}

		public void setBindaddr(String bindaddr) {
			this.bindaddr = bindaddr;
		}

		public String getCertfile() {
			return certfile;
		}

		public void setCertfile(String certfile) {
			this.certfile = certfile;
		}

		public String getCertpass() {
			return certpass;
		}

		public void setCertpass(String certpass) {
			this.certpass = certpass;
		}

	}

	public class Threading {

		private String mode;
		private Integer poolsize;


		public String getMode() {
			return mode;
		}

		public void setMode(String mode) {
			this.mode = mode;
		}

		public Integer getPoolsize() {
			return poolsize;
		}

		public void setPoolsize(Integer poolsize) {
			this.poolsize = poolsize;
		}

	}

	public class Http {

		private Long maxrequestsize;
		private String documentroot;


		public Long getMaxrequestsize() {
			return maxrequestsize;
		}

		public void setMaxrequestsize(Long maxrequestsize) {
			this.maxrequestsize = maxrequestsize;
		}

		public String getDocumentroot() {
			return documentroot;
		}

		public void setDocumentroot(String documentroot) {
			this.documentroot = documentroot;
		}

	}

	public class Acl {

		private String allow;
		private String deny;
		private String order;


		public String getAllow() {
			return allow;
		}

		public void setAllow(String allow) {
			this.allow = allow;
		}

		public String getDeny() {
			return deny;
		}

		public void setDeny(String deny) {
			this.deny = deny;
		}

		public String getOrder() {
			return order;
		}

		public void setOrder(String order) {
			this.order = order;
		}

	}

	public class Rdpoverride {

		private String host;
		private Integer port;
		private String user;
		private String pass;
		private Integer performance;
		private String nowallpaper;
		private String nofullwindowdrag;
		private String nomenuanimation;
		private String notheming;
		private String notls;
		private String nonla;
		private Integer forcentlm;
		private String size;


		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public Integer getPort() {
			return port;
		}

		public void setPort(Integer port) {
			this.port = port;
		}

		public String getUser() {
			return user;
		}

		public void setUser(String user) {
			this.user = user;
		}

		public String getPass() {
			return pass;
		}

		public void setPass(String pass) {
			this.pass = pass;
		}

		public Integer getPerformance() {
			return performance;
		}

		public void setPerformance(Integer performance) {
			this.performance = performance;
		}

		public String getNowallpaper() {
			return nowallpaper;
		}

		public void setNowallpaper(String nowallpaper) {
			this.nowallpaper = nowallpaper;
		}

		public String getNofullwindowdrag() {
			return nofullwindowdrag;
		}

		public void setNofullwindowdrag(String nofullwindowdrag) {
			this.nofullwindowdrag = nofullwindowdrag;
		}

		public String getNomenuanimation() {
			return nomenuanimation;
		}

		public void setNomenuanimation(String nomenuanimation) {
			this.nomenuanimation = nomenuanimation;
		}

		public String getNotheming() {
			return notheming;
		}

		public void setNotheming(String notheming) {
			this.notheming = notheming;
		}

		public String getNotls() {
			return notls;
		}

		public void setNotls(String notls) {
			this.notls = notls;
		}

		public String getNonla() {
			return nonla;
		}

		public void setNonla(String nonla) {
			this.nonla = nonla;
		}

		public Integer getForcentlm() {
			return forcentlm;
		}

		public void setForcentlm(Integer forcentlm) {
			this.forcentlm = forcentlm;
		}

		public String getSize() {
			return size;
		}

		public void setSize(String size) {
			this.size = size;
		}

	}

	public class Openstack {

		private String authurl;
		private String password;
		private String tenantname;


		public String getAuthurl() {
			return authurl;
		}

		public void setAuthurl(String authurl) {
			this.authurl = authurl;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getTenantname() {
			return tenantname;
		}

		public void setTenantname(String tenantname) {
			this.tenantname = tenantname;
		}

	}

	public class Hyperv {

		private String hostusername;
		private String hostpassword;


		public String getHostusername() {
			return hostusername;
		}

		public void setHostusername(String hostusername) {
			this.hostusername = hostusername;
		}

		public String getHostpassword() {
			return hostpassword;
		}

		public void setHostpassword(String hostpassword) {
			this.hostpassword = hostpassword;
		}

	}

}
