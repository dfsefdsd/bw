package olfbrower;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import com.swtdesigner.SWTResourceManager;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;

public class OlfBrowser {
	private long timeout = Integer.parseInt(PropertiesUtil
			.getProperty("flushTime")) * 60000; // 页面刷新时间
	private int checkTime = Integer.parseInt(PropertiesUtil
			.getProperty("checkTime")) * 60000; // 检查更新时间
	private static final String values = "olpf_values";
	// The "at rest" text of the throbber
	private static final String AT_REST = "Ready";
	private boolean login=false;
	private String password =  PropertiesUtil.getProperty("password");
	private HttpClient httpClient = new HttpClient();
	private Shell shell;
	private Display display;
	private boolean autoRecognize = PropertiesUtil.getProperty("autoRecognize").equals("true");
	private Button loginButton;
	private Button defineModuleButton;
	private Button recognizeButton;
	private Button removeUpdateButton;
	private Button autRecognizeButton;
	private String favoriteFolderPath = "favorite";
	private static String index = PropertiesUtil.getProperty("index");
	private CTabFolder pages;
	private static String base = PropertiesUtil.getProperty("base");
	private Menu menu;
	private MenuItem fileButton;
	private Menu fileMenu;
	private MenuItem saveButton;
	private MenuItem editButton;
	private Menu editMenu;
	private MenuItem configButton;
	private Composite controls;
	private Label status;
	private Menu favoriteMenu;
	private ArrayList<Browser> needToBecheckBrowser = new ArrayList<Browser>();
   
	private Image waitImage;
	private Image newImage;
	private Image webImage;
	// private Image folderImage;
	private HashMap<Integer, CTabItem> browserTabs = new HashMap<Integer, CTabItem>();
	private ReFreshtThread refresh = null;
	private static String browserCheckUpdateUserModuleURL = base
			+ "/user/browserCheckUpdateUserModule";
	private static String browserIngoreUpdateURL = base
			+ "/user/browserIngoreUpdate";
	private String userName;
	private NotifyDialog notifyDialog;
	private String userId=PropertiesUtil.getProperty("userId");
	private static final String BLANK = "about:blank";
	private static final String initDiv = "<div name='noaction' style='"
			+ "font-size:3em;;color:#FFFFFF;z-index: 214748366;position: absolute;top:0px;left: 0px;width: 100%;height: 100%;margin: 0px 0px;background-color: #66CCFF;text-align: center;text-decoration: blink;"
			+ "-moz-opacity:0.8;opacity:0.8;filter:progid:DXImageTransform.Microsoft.Alpha(opacity=80);-ms-filter:progid:DXImageTransform.Microsoft.Alpha(opacity=80);"
			+ "'><br/><br/>初始化中，请稍候...</div>";

	private String userDefineDiv=null;	
	private final static String split="￥";
	private boolean configEdited=false;
	private static final String helpPage=PropertiesUtil.getProperty("helpPage");
	/**
	 * 取得当前的浏览器，如果没有页面打开则新建浏览器
	 * 
	 * @return
	 */
	private Browser getCurrentBrowser() {

		if (pages.getSelection() == null) {
			return createABroswer(OlfBrowser.BLANK);
		}
		Control control = pages.getSelection().getControl();
		if (control instanceof Browser == false) {
			return createABroswer(OlfBrowser.BLANK);
		}
		return (Browser) control;
	}

	/**
	 * 订阅模式按钮动作
	 */
	private void defineModule() {

		defineModuleButton.setEnabled(false);
		final Browser browser = getCurrentBrowser();
		BroswerData broswerData = (BroswerData) browser.getData("broswerData");
		if (broswerData.isDefineModule() == true) {
			exitDefineModule(browser);

		} else {
			enterDefindModule(browser);
		}

	}

	private void onload(Browser browser) {
		
		CTabItem tabItem = this.browserTabs.get(browser.hashCode());

		setTitle(browser);
		final BroswerData broswerData = (BroswerData) browser
		.getData("broswerData");
	/*	if(broswerData.getCompletedStatus()==BroswerData.CONPLETED){
			return;
		}*/
		if(browser.getUrl().equals(OlfBrowser.BLANK)){
			broswerData.setCompletedStatus(BroswerData.NO_URL);
			return;
		}
		if (browser.evaluate("return document.location.toString();").toString()
				.equals(browser.getUrl()) == false) {
			// 如果连接失败则返回

			System.out.println("连接" + browser.getUrl() + "失败，网络连接不可用或已跳转");
			broswerData.setCompletedStatus(BroswerData.FAIL);
		
			return;
		}
		
		broswerData.setLastLoadTime(new Date().getTime());
		broswerData.setCompletedStatus(BroswerData.CONPLETED);
		addCssAndScript(browser, tabItem);

	}

	public OlfBrowser() {
		   System.setProperty("proxySet", PropertiesUtil.getProperty("proxySet"));
           System.setProperty("proxyHost",  PropertiesUtil.getProperty("proxyHost"));
           System.setProperty("proxyPort", PropertiesUtil.getProperty("proxyPort"));
           System.setProperty("proxyUsername", PropertiesUtil.getProperty("proxyUsername"));
           System.setProperty("proxyPassword", PropertiesUtil.getProperty("proxyPassword"));

	}

	private void ignoreUpdate() {
		this.httpClient.getMethod(browserIngoreUpdateURL, true);
	}

	/**
	 * 显示通知窗口
	 */
	private void showNotifyDialog() {
		if (notifyDialog == null) {
			notifyDialog = new NotifyDialog(shell, SWT.BORDER);
			notifyDialog.open();

		}
	}

	/**
	 * 新建标签页
	 * 
	 * @param location
	 */
	private Browser createABroswer(String location) {
		defineModuleButton.setEnabled(false);
		recognizeButton.setEnabled(false);
		removeUpdateButton.setEnabled(false);
		final CTabItem tabItem = new CTabItem(pages, SWT.NONE);
		tabItem.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				final Control browser = tabItem.getControl();
				display.asyncExec(new Runnable() {
					public void run() {
						browser.dispose();
					}
				});

			}
		});
		tabItem.setShowClose(true);
		tabItem.setText("\u65B0\u5EFA\u6807\u7B7E\u9875");
		// Create the web browser
		final Browser browser = new Browser(pages, SWT.BORDER);
		browser.setData("broswerData", new BroswerData());

		tabItem.setControl(browser);
		pages.setSelection(tabItem);
		browserTabs.put(browser.hashCode(), tabItem);
		// Create the web browser

		FormData data1 = new FormData();
		data1.left = new FormAttachment(controls, 0, SWT.LEFT);
		data1.top = new FormAttachment(controls);
		data1.bottom = new FormAttachment(status);
		data1.right = new FormAttachment(100, 0);
		browser.setLayoutData(data1);
		browser.addCloseWindowListener(new AdvancedCloseWindowListener());
		browser.addLocationListener(new LocationListener() {
			// The address text box to update
			/**
			 * Called before the location changes
			 * 
			 * @param event
			 *            the event
			 */
			public void changing(LocationEvent event) {
				// Show the location that's loading
				if (tabItem.getImage() == null) {
					tabItem.setImage(waitImage);
					tabItem.setText("正在载入...");
				}
				status.setText("Loading " + event.location + "...");

			}

			/**
			 * Called after the location changes
			 * 
			 * @param event
			 *            the event
			 */
			public void changed(LocationEvent event) {

				urlTextField.setText(getCurrentBrowser().getUrl());

				// Show the loaded location

			}
		});
		browser.addOpenWindowListener(new OpenWindowListener() {
			public void open(WindowEvent event) {
				event.browser = createABroswer(null);

			}
		});

		browser.addProgressListener(new ProgressListener() {
			public void changed(ProgressEvent event) {
			}

			public void completed(ProgressEvent event) {// 访问完成

				onload(browser);

			}
		});
		browser.addStatusTextListener(new AdvancedStatusTextListener(status));

		// 修改源码
		browser.addStatusTextListener(new StatusTextListener() {// 状态栏文字改变事件

					public void changed(StatusTextEvent event) {

						try {
							if (event.text.startsWith(values) == true) {

								final String eventText = event.text
										.substring(values.length());
								browser.execute("window.status=''");
								Thread thread = new Thread() {
									public void run() {
										boolean result = addModule(eventText);
										if (result == true) {
											display.asyncExec(new Runnable() {
												public void run() {
													if(browser.isDisposed()){
														return;
													}
													browser
															.execute("clearTooltips();alert('订阅成功！');");
												}

											});
										} else {
											display.asyncExec(new Runnable() {
												public void run() {
													if(browser.isDisposed()){
														return;
													}
													browser
															.execute("clearTooltips();alert('订阅失败！网络异常或其它原回，请重试！')");
												}
											});

										}
									}
								};
								thread.start();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
		// Go to the initial URL
		if (location != null) {

			if (pages.getSelection().getControl() == browser) {
				urlTextField.setText(location);
			}
			browser.setUrl(location);
			BroswerData broswerData = (BroswerData) browser.getData("broswerData");
			broswerData.setCompletedStatus(BroswerData.LOADING);
		}
		needToBecheckBrowser.add(browser);
		return browser;
	}

	private Button backButton;
	private Button forwordButton;
	private Button flushButton;
	private Button stopButton;
	private Button goButton;
	private Text urlTextField;
	private Text timeoutText;
	private Text checkTimeText;
	private Label label_1;

	/**
	 * Runs the application
	 * 
	 * @param location
	 *            the initial location to display
	 * @wbp.parser.entryPoint
	 */
	public void run() {

		display = new Display();
		shell = new Shell(display);

		shell.setAlpha(250);
		shell.setSize(828, 300);
		shell.setText("关注助手浏览器");
		// shell.setBackgroundImage(SWTResourceManager.getImage("bg.jpg"));
		shell.setBackgroundMode(SWT.INHERIT_FORCE);
		/*
		 * if(userId==null){ LoginFrame }
		 */
		// 创建托盘图标

		ImageData waitImageData = new ImageData("images/loading.gif");
		waitImage = new Image(display, waitImageData);
		ImageData newImageData = new ImageData("images/new.png");
		newImage = new Image(display, newImageData);
		ImageData webImageData = new ImageData("images/web.png");
		webImage = new Image(display, webImageData);

		
		  //托盘
		Tray tray = display.getSystemTray();
		TrayItem item = new TrayItem(tray, SWT.NONE);
		item.setImage(SWTResourceManager.getImage("images/logo.png"));
		item.setToolTipText("互联网平台订阅平台");
		item.setVisible(true);
		item.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				shell.setMaximized(true);
			}

		});
		 
		shell.setImage((SWTResourceManager.getImage("images/logo.png")));
		shell.setMaximized(true);
		// waitImage.dispose();

		// 创建托盘图标

		shell.setLayout(new FormLayout());

		// Create the composite to hold the buttons and text field
		controls = new Composite(shell, SWT.NONE);
		FormData data = new FormData();
		data.top = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		controls.setLayoutData(data);

		// Create the status bar
		status = new Label(shell, SWT.NONE);
		data.left = new FormAttachment(status, 0, SWT.LEFT);
		FormData data2 = new FormData();
		data2.left = new FormAttachment(0, 0);
		data2.right = new FormAttachment(100, 0);
		data2.bottom = new FormAttachment(100, 0);
		status.setLayoutData(data2);
		pages = new CTabFolder(shell, SWT.BORDER);
		pages.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				try {

					Browser browser = getCurrentBrowser();
					urlTextField.setText(browser.getUrl());
					needToBecheckBrowser.add(browser);

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		pages.setSelectionBackground(Display.getCurrent().getSystemColor(
				SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		FormData data3 = new FormData();
		data3.left = new FormAttachment(controls, 0, SWT.LEFT);
		data3.top = new FormAttachment(controls);
		data3.bottom = new FormAttachment(status);
		data3.right = new FormAttachment(100, 0);
		pages.setLayoutData(data3);
		// Create the controls and wire them to the browser
		controls.setLayout(new GridLayout(16, false));

		Button newBrowserButton = new Button(controls, SWT.NONE);
		newBrowserButton.setToolTipText("\u65B0\u5EFA\u6807\u7B7E\u9875");
		newBrowserButton.setImage(SWTResourceManager
				.getImage("images/newBrowser.png"));
		newBrowserButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createABroswer(OlfBrowser.BLANK);
			}
		});

		// Create the back button
		backButton = new Button(controls, SWT.NONE);
		backButton.setToolTipText("\u540E\u9000");
		backButton.setImage(SWTResourceManager.getImage("images/back.png"));
		new Label(controls, SWT.NONE);

		// Create the forward button
		forwordButton = new Button(controls, SWT.PUSH);
		forwordButton.setToolTipText("\u524D\u8FDB");
		forwordButton.setImage(SWTResourceManager
				.getImage("images/forword.png"));

		// Create the refresh button
		flushButton = new Button(controls, SWT.PUSH);
		flushButton.setToolTipText("\u5237\u65B0");
		flushButton.setImage(SWTResourceManager.getImage("images/flush.png"));

		// Create the stop button
		stopButton = new Button(controls, SWT.PUSH);
		stopButton.setToolTipText("\u505C\u6B62");
		stopButton.setImage(SWTResourceManager.getImage("images/stop.png"));

		// Create the address entry field and set focus to it
		urlTextField = new Text(controls, SWT.BORDER);
		urlTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		urlTextField.setFocus();

		// Create the go button
		goButton = new Button(controls, SWT.PUSH);
		goButton.setToolTipText("\u524D\u5F80");
		goButton.setImage(SWTResourceManager.getImage("images/go.png"));

		// Allow users to hit enter to go to the typed URL
		shell.setDefaultButton(goButton);

		// Add event handlers
		loginButton = new Button(controls, SWT.PUSH);
		loginButton.setText("登录(&L)");
		loginButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (login == false) {
					loginButton.setEnabled(false);
					LoginDialog login = new LoginDialog(shell, SWT.DIALOG_TRIM);
					login.open();
					
				} else {
					login = false;
					loginButton.setText("登录(&L)");		
					
					Control []c=pages.getChildren();
					for(int i=0;i<c.length;i++){
						if(c[i] instanceof Browser==false){
							continue;
						}else{
							final Browser browser = (Browser)c[i];
							BroswerData broswerData = (BroswerData) browser.getData("broswerData");
							if(broswerData.isDefineModule()==true){
								exitDefineModule(browser);
							}
						}
					}
					changeButtornStatus();
					
				}

			}
		});

		recognizeButton = new Button(controls, SWT.PUSH);
		recognizeButton.setText("识别更新(&R)");
		recognizeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				final Browser browser = getCurrentBrowser();

				recognizeButton.setEnabled(false);
				recognizeUpdate(browser);

			}
		});
		defineModuleButton = new Button(controls, SWT.PUSH);
		defineModuleButton.setText("订阅模式(&D)");
		defineModuleButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				defineModule();
			}
		});
		removeUpdateButton = new Button(controls, SWT.PUSH);

		removeUpdateButton.setText("标记为已读(&M)");
		removeUpdateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				final Browser browser = getCurrentBrowser();
				removeUpdateButton.setEnabled(false);
				alreadyRead(browser);
			}
		});
		autRecognizeButton = new Button(controls, SWT.CHECK);
		autRecognizeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				autRecognizeButton.setEnabled(false);
				configEdited=true;
				
				if (autRecognizeButton.getSelection() == true) {
					autoRecognize = true;
					Control[] items = pages.getChildren();
					for (int i = 0; i < items.length; i++) {
						final Browser broswer = (Browser) items[i];
						checkModuleExists(broswer);
					}
					PropertiesUtil.setProperty("autoRecognize", "true");
				} else {
					PropertiesUtil.setProperty("autoRecognize", "false");
					autoRecognize = false;
					autRecognizeButton.setEnabled(true);
				}
			}
		});
		autRecognizeButton.setSelection(autoRecognize);
		autRecognizeButton.setText("自动识别(&A)");
		menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);
		fileButton = new MenuItem(menu, SWT.CASCADE);
		fileButton.setText("\u6587\u4EF6(&F)");

		fileMenu = new Menu(fileButton);
		fileButton.setMenu(fileMenu);

		saveButton = new MenuItem(fileMenu, SWT.NONE);
		saveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				final Browser browser = getCurrentBrowser();
				FileDialog save = new FileDialog(shell, SWT.SAVE);
				save.setFilterExtensions(new String[] { "*.htm,*.html" });
				save.setFileName(shell.getText());
				String path;
				if ((path = save.open()) != null) {
					if (!path.endsWith(".htm") && !path.endsWith(".html")
							&& !path.endsWith(".HTM")
							&& !path.endsWith(".HTML")) {
						path += ".html";
					}
					try {
						BufferedWriter out = new BufferedWriter(new FileWriter(
								path));
						out.write(browser.getText());
						out.close();
					} catch (Exception ee) {
						ee.printStackTrace();
					}
				}
			}
		});
		saveButton.setText("\u4FDD\u5B58(&S)");

		configButton = new MenuItem(fileMenu, SWT.NONE);
		configButton.setText("\u9996\u9009\u9879(&C)");

		editButton = new MenuItem(menu, SWT.CASCADE);
		editButton.setText("\u7F16\u8F91(&E)");

		editMenu = new Menu(editButton);
		editButton.setMenu(editMenu);

		MenuItem viewMenu = new MenuItem(menu, SWT.CASCADE);
		viewMenu.setText("\u67E5\u770B(&V)");

		Menu menu_1 = new Menu(viewMenu);
		viewMenu.setMenu(menu_1);

	/*	MenuItem viewSourceMenuItem = new MenuItem(menu_1, SWT.NONE);
		viewSourceMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Control control = pages.getSelection().getControl();
				if (control instanceof Browser) {

					CTabItem tabItem_1 = new CTabItem(pages, SWT.NONE);
					tabItem_1.setShowClose(true);
					tabItem_1.setText(((CTabItem) pages.getSelection())
							.getText()
							+ "-源代码");
					ScrolledComposite scrolledComposite = new ScrolledComposite(
							pages, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

					scrolledComposite.setExpandHorizontal(true);
					scrolledComposite.setExpandVertical(true);

					TextViewer textViewer = new TextViewer(scrolledComposite,
							SWT.BORDER);
					StyledText styledText = textViewer.getTextWidget();
					styledText.setText(((Browser) control).getText());
					scrolledComposite.setContent(styledText);
					scrolledComposite.setMinSize(styledText.computeSize(
							SWT.DEFAULT, SWT.DEFAULT));

					tabItem_1.setControl(scrolledComposite);
					pages.setSelection(tabItem_1);

				}
			}
		});
		viewSourceMenuItem.setText("\u6E90\u4EE3\u7801");*/

		MenuItem toolMenu = new MenuItem(menu, SWT.CASCADE);
		toolMenu.setText("\u5DE5\u5177(&T)");

		Menu menu_2 = new Menu(toolMenu);
		toolMenu.setMenu(menu_2);

		MenuItem favoriteButton = new MenuItem(menu, SWT.CASCADE);
		favoriteButton.setText("\u6536\u85CF\u5939(&Q)");

		favoriteMenu = new Menu(favoriteButton);
		favoriteButton.setMenu(favoriteMenu);

		MenuItem addFavoriteButton = new MenuItem(favoriteMenu, SWT.NONE);
		addFavoriteButton.setImage(SWTResourceManager
				.getImage("images/ButtonAdd.png"));
		// 添加到收藏夹
		addFavoriteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				final Browser browser = getCurrentBrowser();
				String url = browser.getUrl();

				Object obj = browser.evaluate("return document.title");
				if (obj == null)
					return;
				String name = obj.toString();
				File favorite = new File(favoriteFolderPath + "/" + name
						+ ".url");
				try {
					favorite.createNewFile();
					FileOutputStream outputStream = new FileOutputStream(
							favorite);
					DataOutputStream out = new DataOutputStream(outputStream);
					out.writeBytes("[InternetShortcut]\r\nURL="
							+ browser.getUrl());
					out.close();
					MenuItem menuItem = new MenuItem(favoriteMenu, SWT.NONE);
					menuItem.setImage(webImage);
					menuItem.setText(name);
					final String turl = new String(browser.getUrl());
					menuItem.setData("url", url);
					menuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent event) {

							createABroswer(turl);
						}
					});
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		addFavoriteButton.setText("\u6DFB\u52A0\u5230\u6536\u85CF\u5939");
		createABroswer(index.equals("")?null:index);

		this.addFavorites(favoriteMenu, favoriteFolderPath);

		MenuItem helpMenu = new MenuItem(menu, SWT.CASCADE);
		helpMenu.setText("\u5E2E\u52A9(&H)");

		Menu menu_3 = new Menu(helpMenu);
		helpMenu.setMenu(menu_3);
		//
		MenuItem viewHelp = new MenuItem(menu_3, SWT.NONE);
		viewHelp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createABroswer(helpPage);
			}
		});
		viewHelp.setText("查看帮助");
		//
		
		backButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				getCurrentBrowser().back();
			}
		});
		forwordButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				getCurrentBrowser().forward();
			}
		});
		flushButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				Browser tbrowser = getCurrentBrowser();

				tbrowser.setUrl(tbrowser.getUrl());
				BroswerData broswerData = (BroswerData) tbrowser.getData("broswerData");
				broswerData.setCompletedStatus(BroswerData.LOADING);
				needToBecheckBrowser.add(tbrowser);

			}
		});
		stopButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {

				getCurrentBrowser().stop();

			}
		});
		goButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				try {
					getCurrentBrowser().setUrl(urlTextField.getText());
					BroswerData broswerData = (BroswerData) getCurrentBrowser().getData("broswerData");
					broswerData.setCompletedStatus(BroswerData.LOADING);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});
		defineModuleButton.setEnabled(false);
		recognizeButton.setEnabled(false);
		autRecognizeButton.setEnabled(false);
		removeUpdateButton.setEnabled(false);

		label_1 = new Label(controls, SWT.NONE);
		label_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false,
				1, 1));
		label_1.setText("\u5237\u65B0|\u68C0\u67E5\u65F6\u95F4");

		timeoutText = new Text(controls, SWT.BORDER);
		timeoutText.setSize(50, 50);
		timeoutText.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				Pattern pattern = Pattern.compile("[0-9]\\d*");
				Matcher matcher = pattern.matcher(timeoutText.getText());
				if (matcher.matches()) { // 处理数字
					timeout = Integer.parseInt(timeoutText.getText()) * 60000;
					configEdited=true;
					PropertiesUtil.setProperty("flushTime", timeoutText.getText());
				} else {
					timeoutText.setText(timeout / 60000 + "");
				}
				
			}
		});
		timeoutText.setText(timeout / 60000 + "");

		checkTimeText = new Text(controls, SWT.BORDER);
		timeoutText.setSize(50, 50);
		checkTimeText.setText(checkTime / 60000 + "");
		checkTimeText.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				Pattern pattern = Pattern.compile("[0-9]\\d*");
				Matcher matcher = pattern.matcher(checkTimeText.getText());
				if (matcher.matches()) { // 处理数字
					int temp = Integer.parseInt(checkTimeText.getText()) * 60000;
					configEdited=true;
					PropertiesUtil.setProperty("checkTime", checkTimeText.getText());
				
					if (temp != checkTime) {
						checkTime = temp;
						refresh.interrupt();
					}
				} else {
					checkTimeText.setText(checkTime / 60000 + "");
				}
			}
		});
		shell.open();
		if (password.length()<1||userId.length()<1) {

			LoginDialog login = new LoginDialog(shell, SWT.DIALOG_TRIM);
			login.open();

		} else{
			// final Browser browser = (Browser)
			// pages.getSelection().getControl();
			Thread thread = new Thread() {
				public void run() {
					handelLogin(display, userId, password);
				}
			};
			thread.start();
		}
		shell.addDisposeListener(new DisposeListener(){
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				// TODO Auto-generated method stub
				if(configEdited){
					PropertiesUtil.save();
				}
			}		
		});
		while (!shell.isDisposed()) {
			if (!display.isDisposed()&&!display.readAndDispatch()) {
				if (needToBecheckBrowser.size() > 0) {
					changeButtornStatus();
				}
				display.sleep();
			}
		}
		if (refresh != null) {
			refresh.setKeepRun(false);
		}
		newImage.dispose();
		waitImage.dispose();
		webImage.dispose();

		display.dispose();
	}

	/**
	 * 加入收藏的网址
	 * 
	 * @param menu_3
	 * @param none
	 */
	private void addFavorites(Menu parentMenu, String path) {
		// TODO Auto-generated method stub
		File favoriteFolder = new File(path);
		if (favoriteFolder.exists() == false) {
			favoriteFolder.mkdirs();
			return;
		} else {
			File[] favorites = favoriteFolder.listFiles(new FileFilter() {

				@Override
				public boolean accept(File file) {
					return file.isFile();
				}
			});
			MenuItem menuItem;
			// 加入打开所有链接
			menuItem = new MenuItem(parentMenu, SWT.NONE);
			menuItem.setText("打开所有链接");
			menuItem.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent event) {
					MenuItem menuItem = (MenuItem) event.getSource();
					Menu parent = menuItem.getParent();
					MenuItem menuItems[] = parent.getItems();
					Object url = null;
					for (int i = 0; i < menuItems.length; i++) {
						url = menuItems[i].getData("url");
						if (url != null) {
							createABroswer(url.toString());
						}
					}
				}
			});

			// 加入打开所有链接
			String name;
			FileInputStream in;
			String tempurl;
			int beginIndex;
			for (int i = 0; i < favorites.length; i++) {

				menuItem = new MenuItem(parentMenu, SWT.NONE);

				menuItem.setImage(webImage);
				name = favorites[i].getName();
				if (name.endsWith(".url")) {
					menuItem.setText(name.substring(0, name.length() - 4));
				} else {
					menuItem.setText(name);
				}
				in = null;
				try {
					in = new FileInputStream(favorites[i]);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				byte tempb[] = new byte[1024];
				int n = 0;
				try {
					n = in.read(tempb);
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (n < 0) {
					continue;
				}
				tempurl = new String(tempb, 0, n);
				beginIndex = tempurl.indexOf("URL=");
				if (beginIndex > 0 && (beginIndex + 4) < tempurl.length()) {
					final String url = tempurl.substring(beginIndex + 4);
					menuItem.setData("url", url);
					menuItem.addSelectionListener(new SelectionAdapter() {

						public void widgetSelected(SelectionEvent event) {
							createABroswer(url);
						}
					});
				}
			}
			favorites = favoriteFolder.listFiles(new FileFilter() {

				@Override
				public boolean accept(File file) {
					return file.isDirectory();
				}
			});
			for (int i = 0; i < favorites.length; i++) {

				menuItem = new MenuItem(parentMenu, SWT.CASCADE);
				menuItem.setImage(SWTResourceManager
						.getImage("images/folder.png"));
				name = favorites[i].getName();
				menuItem.setText(name);

				Menu newMenu = new Menu(menuItem);
				menuItem.setMenu(newMenu);

				this.addFavorites(newMenu, favorites[i].getAbsolutePath());

				// 未完成下层收藏夹

			}
		}
	}

	/**
	 * Creates the main window's contents
	 * 
	 * @param shell
	 *            the main window
	 * @param location
	 *            the initial location
	 */
	// public void createContents(final Shell shell, String location)

	/**
	 * This class implements a CloseWindowListener for AdvancedBrowser
	 */
	class AdvancedCloseWindowListener implements CloseWindowListener {
		/**
		 * Called when the parent window should be closed
		 */
		public void close(WindowEvent event) {
			// Close the parent window
			((Browser) event.widget).getShell().close();
		}
	}

	/**
	 * This class implements a ProgressListener for AdvancedBrowser
	 */
	class AdvancedProgressListener implements ProgressListener {
		// The label on which to report progress
		private Label progress;

		/**
		 * Constructs an AdvancedProgressListener
		 * 
		 * @param label
		 *            the label on which to report progress
		 */
		public AdvancedProgressListener(Label label) {
			// Store the label on which to report updates
			progress = label;
		}

		/**
		 * Called when progress is made
		 * 
		 * @param event
		 *            the event
		 */
		public void changed(ProgressEvent event) {
			// Avoid divide-by-zero
			if (event.total != 0) {
				// Calculate a percentage and display it
				int percent = (int) (event.current / event.total);
				progress.setText(percent + "%");
			} else {
				// Since we can't calculate a percent, show confusion :-)
				progress.setText("完成");
			}
		}

		/**
		 * Called when load is complete
		 * 
		 * @param event
		 *            the event
		 */
		public void completed(ProgressEvent event) {
			// Reset to the "at rest" message
			progress.setText(AT_REST);

		}
	}

	/**
	 * This class implements a StatusTextListener for AdvancedBrowser
	 */
	class AdvancedStatusTextListener implements StatusTextListener {
		// The label on which to report status
		private Label status;

		/**
		 * Constructs an AdvancedStatusTextListener
		 * 
		 * @param label
		 *            the label on which to report status
		 */
		public AdvancedStatusTextListener(Label label) {
			// Store the label on which to report status
			status = label;
		}

		/**
		 * Called when the status changes
		 * 
		 * @param event
		 *            the event
		 */
		public void changed(StatusTextEvent event) {
			// Report the status
			if (event.text.startsWith(values) == true) {
				return;
			}
			status.setText(event.text);
		}
	}

	/**
	 * The application entry point
	 * 
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		new OlfBrowser().run();

	}

	/**
	 * 配置http连接的属性，防止被封
	 * 
	 * @param url_c
	 *            http连接
	 */
	public static void setHeader(HttpURLConnection url_c) {
		try {
			url_c
					.setRequestProperty(
							"User-Agent",
							"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; InfoPath.2; CIBA; MAXTHON 2.0)");
			url_c.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");

			// 设置只接收文本，不接收其余多媒体
			url_c.setRequestProperty("Accept", "*/*");
			url_c.setRequestProperty("Connection", "keep-alive");
			url_c.setRequestProperty("Pragma", "no-cache");
			url_c.setRequestProperty("Cache-Control", "no-cache");
			url_c.setUseCaches(false);
			url_c.setRequestMethod("GET");
			HttpURLConnection.setFollowRedirects(true);
			url_c.setInstanceFollowRedirects(true);
			url_c.setRequestProperty("Accept-Encoding", "gzip"); // 接收压缩内容
			url_c.setDoOutput(true);
			url_c.setDoInput(true);
			url_c.setRequestMethod("POST");
			url_c.setUseCaches(false);
			url_c.setConnectTimeout(0);
			url_c.setReadTimeout(0);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}



	private boolean addModule(String values) {
		String defineDiv = null;
		try {
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("userId", URLEncoder.encode(userId, "utf-8"));
			parameters.put("password", URLEncoder.encode(password, "utf-8"));
			parameters.put("values", URLEncoder.encode(values, "utf-8"));
			defineDiv = this.post(base + "/user/addModuleByBrower", parameters);
			if (defineDiv!=null&&(defineDiv.equals("success") == true)||defineDiv.startsWith("<html>")) {
				return true;
			} else {
				System.out.println(defineDiv);
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("出错了");
			return false;
		}
	}

	/**
	 * 识别更新
	 * 
	 * @return
	 */
	private void recognizeUpdate(final Browser browser) {
		if (browser.isDisposed()) {
			System.out.println("浏览器已Disposed");
			return;
		}
		final BroswerData broswerData = (BroswerData) browser
				.getData("broswerData");
		if (broswerData.getPaths().equals("null")) {
			System.out.println("模块路径为空");
			return;
		}
		browserTabs.get(browser.hashCode()).setImage(waitImage);
		browserTabs.get(browser.hashCode()).setText("正在识别...");
		final String html = browser.evaluate("return document.body.innerHTML;")
				.toString();
		final String urlstr = browser.getUrl().toString();

		Thread thread = new Thread() {
			public void run() {
				String defineDiv = null;
				Map<String, String> para = new HashMap<String, String>();
				try {
					para.put("userId", URLEncoder.encode(userId, "utf-8"));
					para.put("password", URLEncoder.encode(password, "utf-8"));
					para.put("url", URLEncoder.encode(urlstr, "utf-8"));
					para.put("html", URLEncoder.encode(html, "utf-8"));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				defineDiv = post(base + "/user/recognizeUpdate", para);
				if (defineDiv.equals("failLogin") == false) {
					final Style values = new Style(defineDiv);
					display.asyncExec(new Runnable() {
						public void run() {
							if(browser.isDisposed()){
								return;
							}
							broswerData.setUpdatePaths(values
									.getAStyle("updatePaths"));
							System.out.println("更新路径："
									+ broswerData.getUpdatePaths());
							
							if (broswerData.getUpdatePaths().equals("null") == false) {
								broswerData.setUserModuleId(values
										.getAStyle("userModuleId"));
								broswerData.setUpdatePaths(broswerData
										.getUpdatePaths().replaceAll("/", ">")
										.replaceAll("\\[", ":eq(").replaceAll(
												"\\]", ")"));
								markUpdate(browser);
							}
							needToBecheckBrowser.add(browser);
						}
					});
				}
			}
		};
		thread.start();
	}

	/**
	 * 标识更新
	 */
	private void markUpdate(final Browser browser) {

		BroswerData broswerData = (BroswerData) browser.getData("broswerData");
		// TODO Auto-generated method stub

		boolean result = browser.execute("markUpdate('"
				+ broswerData.getUpdatePaths() + "');");
		System.out.println("标记更新" + result);
	}

	private void setTitle(Browser browser) {
		CTabItem tabItem = browserTabs.get(browser.hashCode());
		tabItem.setImage(webImage);
		Object obj = browser.evaluate("return document.title;");
		if (obj == null) {
			return;
		}

		String title = obj.toString();

		if (title.length() == 0) {
			title = "新标签页";
		}
		tabItem.setText(title);
	}

	/**
	 * 检测模块是否存在
	 * 
	 * @param urlstr
	 * @param path
	 * @return
	 */
	private void checkModuleExists(final Browser browser) {
        if(browser.getUrl()==null||browser.getUrl().length()<8){
        	return;
        }
		final BroswerData broswerData = (BroswerData) browser
				.getData("broswerData");
		broswerData.setUserModuleId("null");
		broswerData.setPaths("null");
		broswerData.setUpdatePaths("null");
		final String urlstr = browser.getUrl().toString();
		browserTabs.get(browser.hashCode()).setImage(waitImage);
		browserTabs.get(browser.hashCode()).setText("正在识别...");
		try {
			Thread thread = new Thread() {
				public void run() {
					String defineDiv = null;
					try {
						defineDiv = httpClient.getMethod(base
								+ "/user/checkModuleExists?url="
								+ URLEncoder.encode(urlstr, "utf-8"), true);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if (defineDiv.toLowerCase().startsWith("null") == false) {
						final String tempstr = defineDiv;
						display.asyncExec(new Runnable() {
							public void run() {
								if(browser.isDisposed()){
									return;
								}
								broswerData.setPaths(tempstr);
								System.out.println("存在模块：" + true);
								browser.execute("window.markModules('solid 2px #FF0000',\""
										+ broswerData.getPaths().replaceAll("\"", "'") + "\")");
								recognizeUpdate(browser);
							}
						});

					} else {
						broswerData.setUserModuleId("null");
						broswerData.setPaths("null");
						display.asyncExec(new Runnable() {
							public void run() {
								if(browser.isDisposed()){
									return;
								}
								setTitle(browser);
							}
						});
					}
				}
			};
			thread.start();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 检查页面更新
	 * 
	 * @param urlstr
	 * @return 0为没有模块，1为更新，2为没有更新
	 */
	private int checkModuleUpdatePaths(String urlstr) {
		String defineDiv = null;
		try {
			/*
			 * defineDiv=this.httpClient.getMethod(base +
			 * "/user/checkModuleUpdatePaths?url=" + URLEncoder.encode(urlstr,
			 * "utf-8") + "&path=" + URLEncoder.encode("html[0]/body[0]",
			 * "utf-8"),true); if (defineDiv.startsWith("false")) {
			 * this.userModuleId = "null"; this.contentId = "null"; updatePaths
			 * = "null"; return 0;// 没有模块 } else { Style values=new
			 * Style(defineDiv); this.userModuleId
			 * =values.getAStyle("userModuleId"); this.contentId =
			 * values.getAStyle("contentId"); this.updatePaths =
			 * values.getAStyle("updatePaths"); if
			 * (this.contentId.equals("null") == false) { return 1; } else {
			 * return 2; } }
			 */
			return 0;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}

	private void enterDefindModule(final Browser browser) {

		
		final BroswerData broswerData = (BroswerData) browser
				.getData("broswerData");
		if(broswerData.getCompletedStatus()==BroswerData.NO_URL){
			alert("当前为空白页，不能订阅！");
			needToBecheckBrowser.add(browser);
			return;
		}
		if(broswerData.getCompletedStatus()==BroswerData.FAIL){
			alert("当前为空白页，不能订阅！");
			needToBecheckBrowser.add(browser);
			return;
		}
		if(broswerData.getCompletedStatus()==BroswerData.LOADING){
			alert("当前页面未载入完毕，请稍候再订阅！");
			needToBecheckBrowser.add(browser);
			return;
		}
		boolean exeits = (Boolean) browser
				.evaluate("return document.getElementById('defineTable')!=null;");
		boolean result=false;
		if (exeits) {
		     result = browser
			.execute("document.getElementById('defineTable').innerHTML='"+userDefineDiv+"'");
		}else{
			 result = browser
				.execute("scrip=document.createElement('div');"
						+ "scrip.innerHTML=\""
						+ userDefineDiv
						+ "\";document.body.appendChild(scrip);");
		       
		}
		System.out.println("加入div:" + result);
		exeits = (Boolean) browser
				.evaluate("return (typeof checkDefineDiv!='undefined')");
		if (exeits) {
			 result = browser
				.execute("setTimeout(checkDefineDiv, checkTime);");
			
		}else{
		    result = browser
				.execute("var scrip=document.createElement('script');"
						+ "scrip.type='text/javascript';"
						+ "scrip.src='"
						+ base
						+ "/js/broswerjs/beginDefine.js';document.getElementsByTagName('head')[0].appendChild(scrip);");
		}	
		System.out.println("beginDefine.js:" + result);	     
		// TODO Auto-generated method stub
		checkInit(shell, browser);
		if(broswerData.getCompletedStatus()==BroswerData.FAIL){
			alert("当前面为空白页，不能定义！");
		}
		defineModuleButton.setText("退出订阅(&B)");

		broswerData.setDefineModule(true);
		needToBecheckBrowser.add(browser);
		return;
	}

	private boolean addInitDiv(final Browser browser){
		boolean exeits = (Boolean) browser
		.evaluate("return document.getElementById('initDiv')!=null;");
		boolean result=false;
		if(exeits){
			result=browser.execute("document.getElementById('initDiv').innerHTML=\""+initDiv+"\";");
		}else{
			
			result=browser.execute("scrip=document.createElement('div');"
					+ "scrip.innerHTML=\"<div name='noaction' id='initDiv'>'" + initDiv
					+ "</div>\";document.body.appendChild(scrip);");
			}
        return result;
	}
	private void checkInit(final Shell shell2, final Browser browser) {
		// 加入等待层
		boolean result=false;
		result=addInitDiv(browser);
		Thread thread = new Thread() {

			public void run() {
				final StringBuilder inited = new StringBuilder();
				for (; inited.length() < 1;) {
					if (browser.isDisposed()) {
						return;
					}
					if (shell.getDisplay().isDisposed()) {
						return;
					}
					shell.getDisplay().syncExec(new Runnable() {
						public void run() {
							if (browser.isDisposed()) {
								inited.append("disposed");
								return;
							}
							BroswerData broswerData = (BroswerData) browser.getData("broswerData");
							if(broswerData.getCompletedStatus()==BroswerData.LOADING){
								
								return;
							}
							if(broswerData.getCompletedStatus()==BroswerData.FAIL){
								inited.append("FAIL");
								return;
							}
							boolean exeits = (Boolean) browser
									.evaluate("return document.getElementById('defineTable')!=null;");
							
							if (!exeits) {
								System.out.println("定义div不存在");
								return;
							}
							exeits = (Boolean) browser
									.evaluate("return (typeof jQuery!='undefined')");
							if (!exeits) {
								System.out.println("jQuery不存在");
								return;
							}
							exeits = (Boolean) browser
									.evaluate("return (typeof getURL!='undefined')");
							if (!exeits) {
								System.out.println("browerToolkits.js不存在");
								return;
							}
							exeits = (Boolean) browser
									.evaluate("return (typeof defineIframe!='undefined')");
							if (!exeits) {
								System.out.println("defineIframe不存在");
								return;
							}
							exeits = (Boolean) browser
									.evaluate("return (typeof getAPath!='undefined')");
							if (!exeits) {
								System.out.println("feature.js不存在");
								return;
							}
							exeits = (Boolean) browser
									.evaluate("return (typeof unBindOthers!='undefined')");
							if (!exeits) {
								System.out.println("browerDefineJs.js不存在");
								return;
							}
							exeits = (Boolean) browser
									.evaluate("return (typeof checkDefineDiv!='undefined')");
							if (!exeits) {
								System.out.println("beginDefine.js不存在");
								return;
							}
							exeits = (Boolean) browser
									.evaluate("return (typeof ready!='undefined')&&(ready==true)");
							if (!exeits) {
								System.out.println("ready==false");
								return;
							}
							inited.append("exeits");
						/*	if (browser.isDisposed() == false) {
								browser
										.execute("document.getElementById('initDiv').innerHTML=''");
							}*/
						}

					});
					if (inited.length() < 1) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}
			
			}

		};
		thread.start();
	}

	private void exitDefineModule(final Browser browser) {
		// TODO Auto-generated method stub
		defineModuleButton.setText("订阅模式(&D)");

		final BroswerData broswerData = (BroswerData) browser.getData("broswerData");
		System.out.println("退出订阅加入initdiv："+addInitDiv(browser));
		display.asyncExec(new Runnable() {
			public void run() {
				if (browser.isDisposed()) {
					return;
				}
				if (broswerData.isDefineModule() == true) {
					boolean result = browser
							.execute("window.clearTooltips();window.clearElement();window.unBind(jQuery('body'));ready=false;document.getElementById('initDiv').innerHTML='';alert('已退出订阅模式');");
					System.out.println("退出" + result);
				}
				broswerData.setDefineModule(false);
				needToBecheckBrowser.add(browser);
			}

		});
		
	}

	/**
	 * 标记为已读
	 * 
	 * @return
	 */
	private void alreadyRead(final Browser browser) {
		final BroswerData broswerData = (BroswerData) browser
				.getData("broswerData");
		try {

			final String userModuleId = broswerData.getUserModuleId();
			Thread thread = new Thread() {
				public void run() {
					try {
						httpClient.getMethod(base
								+ "/mobile/alreadyRead?userModuleId="
								+ URLEncoder.encode(userModuleId, "UTF-8"),
								true);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					display.asyncExec(new Runnable() {
						public void run() {
							if (browser.isDisposed()) {
								return;
							}
							browser.execute("removeUpdate();");
							broswerData.setUserModuleId("null");
							broswerData.setUpdatePaths("null");
							needToBecheckBrowser.add(browser);
						}

					});

				}
			};
			thread.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 加入基本的js
	 */
	private void addBaseJs(final Browser browser) {

		Object obj = null;
		try {
			obj = browser.evaluate("return window.olpfBase;");

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (obj != null) {
			return;
		} else {
			boolean result = browser
					.execute("var scrip=document.createElement('script');"
							+ "scrip.type='text/javascript';"
							+ "scrip.src='"
							+ base
							+ "/js/broswerjs/baseJs.js';document.getElementsByTagName('head')[0].appendChild(scrip);");
			System.out.println("加入基本js:" + result);
			// 加入toolkits.js
			result = browser
					.execute("scrip=document.createElement('script');"
							+ "scrip.type='text/javascript';"
							+ "scrip.src='"
							+ base
							+ "/js/broswerjs/browerToolkits.js';document.getElementsByTagName('head')[0].appendChild(scrip);");
			result = browser
					.execute("scrip=document.createElement('script');"
							+ "scrip.type='text/javascript';"
							+ "scrip.src='"
							+ base
							+ "/js/userindexjs/defineIframe.js';document.getElementsByTagName('head')[0].appendChild(scrip);");
			System.out.println("defineIframe.js:" + result);
			result = browser
			.execute("scrip=document.createElement('script');"
					+ "scrip.type='text/javascript';"
					+ "scrip.src='"
					+ base
					+ "/js/common/feature.js';document.getElementsByTagName('head')[0].appendChild(scrip);");
	        System.out.println("加入feature.js:" + result);
			result = browser
					.execute("scrip=document.createElement('script');"
							+ "scrip.type='text/javascript';"
							+ "scrip.src='"
							+ base
							+ "/js/broswerjs/browerDefineJs.js';document.getElementsByTagName('head')[0].appendChild(scrip);");
			System.out.println("加入browerDefineJs.js:" + result);
		}
	}

	/**
	 * 加入jquery
	 */
	private void addCSS(final Browser browser) {

		Object obj = null;
		try {
			obj = browser
					.evaluate("return document.getElementById('olpfcss')==null;");
			System.out.println(obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (obj != null && (Boolean) obj == false) {
			return;
		} else {
			// 加入css
			boolean result = browser
					.execute("var scrip=document.createElement('link');"
							+ "scrip.rel='stylesheet';scrip.id='olpfcss';"
							+ "scrip.type='text/css';"
							+ "scrip.href='"
							+ base
							+ "/css/userindexcss/olpfmaster.css';document.getElementsByTagName('head')[0].appendChild(scrip);");
			System.out.println("加入css:" + result);

		}
	}

	/**
	 * 加入jquery
	 */
	private void addJquery(final Browser browser) {

		Object obj = null;
		try {
			obj = browser.evaluate("return window.jQuery;");

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (obj != null) {
			return;
		} else {

			// 加入jquery
			boolean result = browser
					.execute("scrip=document.createElement('script');"
							+ "scrip.type='text/javascript';"
							+ "scrip.src='"
							+ base
							+ "/js/userindexjs/olpfjquery-1.3.2.min.js';document.getElementsByTagName('head')[0].appendChild(scrip);");
			System.out.println("加入jquery:" + result);
		}
	}

	/**
	 * 登录，成功返回null.失败返回失败信息
	 * @param userid
	 * @param password
	 * @return
	 */
	private String login(String userid, String password) {
		String result=null;
	
		try{
		 result = this.httpClient.getMethod(base
				+ "/user/broswerLogin?userid=" + userid + "&password="
				+ password, true);
		}catch(Exception e){
			e.printStackTrace();
			login=false;
			return "用户或密码出错，请重新登录！";
		}
		if (result.startsWith("1")) {
            this.password=password;
			login=true;
			String []args=result.split(OlfBrowser.split);
			System.out.println("args.length:"+args.length );
			userName = args[1];
			userDefineDiv=args[2].replaceAll("\\s+", " ").replaceAll("\"", "'");
			// System.out.println(userName);
			return null;
		} else if (result.startsWith("0")) {
			String []args=result.split(OlfBrowser.split);
			return args[1];
		}else{
			return "未知错误";
		}

	}

	/**
	 * 
	 * @param urlstr
	 * @param parameters
	 * @return
	 */
	private String post(String urlstr, Map parameters) {
		try {
			URL url = new URL(urlstr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			setHeader(conn); // 设置请求头
			conn.connect();
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			// The URL-encoded contend
			// 正文，正文内容其实跟get的URL中'?'后的参数字符串一致
			StringBuffer postParams = new StringBuffer();
			int index = 0;
			for (Iterator<Entry> iter = parameters.entrySet().iterator(); iter
					.hasNext(); index++) {
				Entry<String, String> entry = iter.next();
				postParams.append(index != 0 ? "&" : "");
				postParams.append(entry.getKey());
				postParams.append("=");
				postParams.append(entry.getValue());
			}
			String content = postParams.toString();
			// DataOutputStream.writeBytes将字符串中的16位的 unicode字符以8位的字符形式写道流里面
		/*	int length=1024*1024;
			int i=0;
			for(i=0;i<content.length()/length;i++){
				out.writeBytes(content.substring(i*length,(i+1)*length));
				out.flush();
			}
			if(content.length()>i*length){*/
			out.writeBytes(content);
			out.flush();
			
			out.close(); // flush and close
			byte[] data = new byte[1024];
			InputStream in = null;
			String encoding = conn.getContentEncoding();// 检查页面是否返回gzip编码

			if (encoding != null && encoding.indexOf("gzip") >= 0) { // 如果页面返回gzip编码，则用GZIPInputStream读取并解码
				// System.out.println("页面编码" + conn.getContentEncoding());
				in = new BufferedInputStream(new GZIPInputStream(conn
						.getInputStream()), 1024);
			} else {
				in = new BufferedInputStream(conn.getInputStream(), 1024);
			}
			int n = 1;
			StringBuilder temp = new StringBuilder(1024);
			while (n > 0) {
				n = in.read(data);
				if (n > 0) {
					temp.append(new String(data, 0, n));
				}
			}
			in.close();
			conn.disconnect();
			 System.out.println(url+"  POST回应："+temp.toString());
			return temp.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * 检测模块是否存在
	 * 
	 * @param urlstr
	 * @param path
	 * @return
	 */
	private String get(String urlstr) {
		String defineDiv = null;
		try {
			URL url = new URL(urlstr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			setHeader(conn); // 设置请求头
			conn.connect();
			byte[] data = new byte[1024];
			InputStream in = null;
			String encoding = conn.getContentEncoding();// 检查页面是否返回gzip编码

			if (encoding != null && encoding.indexOf("gzip") >= 0) { // 如果页面返回gzip编码，则用GZIPInputStream读取并解码
				// System.out.println("页面编码" + conn.getContentEncoding());
				in = new BufferedInputStream(new GZIPInputStream(conn
						.getInputStream()), 1024);
			} else {
				in = new BufferedInputStream(conn.getInputStream(), 1024);
			}
			int n = 1;
			StringBuilder temp = new StringBuilder(1024);
			while (n > 0) {
				n = in.read(data);
				if (n > 0) {
					temp.append(new String(data, 0, n));
				}
			}
			in.close();
			conn.disconnect();
			defineDiv = temp.toString();
			return defineDiv;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	// 内部登录框
	public class LoginDialog extends Dialog {

		protected Object result;
		protected Shell shell;
		private Text userInputText;
		private Text passwordInputText;

		/**
		 * Create the dialog.
		 * 
		 * @param parent
		 * @param style
		 */
		public LoginDialog(Shell parent, int style) {
			super(parent, style);
			setText("登录");
		}

		/**
		 * Open the dialog.
		 * 
		 * @return the result
		 */
		public Object open() {
			createContents();
			shell.setBounds(new Rectangle(((int) Toolkit.getDefaultToolkit()
					.getScreenSize().getWidth() - 266) / 2,
					((int) Toolkit.getDefaultToolkit().getScreenSize()
							.getHeight() - 200) / 2, 266, 200));
			shell.open();
			shell.layout();
			shell.addDisposeListener(new DisposeListener(){

				@Override
				public void widgetDisposed(DisposeEvent arg0) {
					// TODO Auto-generated method stub
					loginButton.setEnabled(true);
				}
				
			});
			Display display = getParent().getDisplay();
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			
			return result;
		}

		/**
		 * Create contents of the dialog.
		 */
		private void createContents() {
			shell = new Shell(getParent(), SWT.CLOSE | SWT.TITLE);
			shell.setSize(262, 207);
			shell.setText("\u767B\u5F55");

			Label userLabel = new Label(shell, SWT.NONE);
			userLabel.setFont(SWTResourceManager.getFont("Tahoma", 12,
					SWT.NORMAL));
			userLabel.setBounds(30, 40, 38, 19);
			userLabel.setText("\u8D26\u53F7:");

			Label passwordLabel = new Label(shell, SWT.NONE);
			passwordLabel.setFont(SWTResourceManager.getFont("Tahoma", 12,
					SWT.NORMAL));
			passwordLabel.setBounds(30, 75, 44, 19);
			passwordLabel.setText("\u5BC6\u7801:");

			userInputText = new Text(shell, SWT.BORDER);
			userInputText.setBounds(76, 40, 133, 20);
			userInputText.setText(userId);
			passwordInputText = new Text(shell, SWT.BORDER | SWT.PASSWORD);
			passwordInputText.setBounds(76, 75, 133, 20);
			passwordInputText.setText(password);
			final Button saveLogin = new Button(shell, SWT.CHECK);
			if(PropertiesUtil.getProperty("password").length()>0){
				saveLogin.setSelection(true);
			}
			saveLogin.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					configEdited=true;
					if(saveLogin.getSelection()){
						PropertiesUtil.setProperty("password", passwordInputText.getText());
						password=passwordInputText.getText();
					}else{
						PropertiesUtil.setProperty("password", "");
						password="";
						
					}
					PropertiesUtil.setProperty("userId",userInputText.getText());
					
					userId=userInputText.getText();
				
				}
			});
		
			saveLogin.setBounds(76, 141, 98, 17);
			saveLogin.setText("保存密码");
			Button iLoginButton = new Button(shell, SWT.NONE);
			iLoginButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					final String user = userInputText.getText();
					final String tpassword = passwordInputText.getText();
					if(user.length()==0){
						alert("用户名不能 为空！");
						return;
					}
					if(tpassword.length()==0){
						alert("密码不能 为空！");
						return;
					}
					loginButton.setEnabled(false);					
                    PropertiesUtil.setProperty("userId", user);
                    userId=user;
                    if(saveLogin.getSelection()){
                       PropertiesUtil.setProperty("password", tpassword);
                       password=tpassword;
                    }else{
                    	PropertiesUtil.setProperty("password", "");
                        password="";
                    }
					Thread thread = new Thread() {
						public void run() {
							handelLogin(display, user, tpassword);
						}
					};
					thread.start();
					shell.dispose();
				}

			});
			iLoginButton.setBounds(40, 111, 79, 24);
			iLoginButton.setText("登录(&L)");
			Button registerButton = new Button(shell, SWT.NONE);
			registerButton.setBounds(135, 111, 79, 24);
			registerButton.setText("注册(&R)");
			registerButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					createABroswer(PropertiesUtil.getProperty("registerURL"));
					shell.dispose();
					loginButton.setEnabled(true);
				}
			});
			
		}

		}

	private void handelLogin(Display display, String user, String tpassword) {

		String result = "";
		try {
			result = login(user, tpassword); // TODO add
		} catch (Exception e) {
			e.printStackTrace();
			result=e.getMessage();
		}
		if (result == null) {
			if (refresh == null) {
				refresh = new ReFreshtThread();
				refresh.start();
			} else if (refresh.getState() == Thread.State.TERMINATED) {
				refresh = new ReFreshtThread();
				refresh.start();
			}
			if (autoRecognize == true) {
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						loginButton.setEnabled(true);
						Control[] items = pages.getChildren();

						for (int i = 0; i < items.length; i++) {

							if (items[i] instanceof Browser == false) {
								continue;
							}
							final Browser broswer = (Browser) items[i];
							if (broswer.isDisposed()) {
								return;
							}
							checkModuleExists(broswer);
						}
					}
				});
			} 
			
		} else {
			
			final String result1=result;
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					loginButton.setText("登录(&L)");
					loginButton.setEnabled(true);
					alert(result1);
				}
			});
		}
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if(login){
					loginButton.setText("退出(&E)");
				}
				needToBecheckBrowser.add(getCurrentBrowser());
			}
		});
		

	}

	/**
	 * 改变按钮状态
	 */
	private void changeButtornStatus() {
		if (login==false) {
			defineModuleButton.setEnabled(false);
			recognizeButton.setEnabled(false);
			autRecognizeButton.setEnabled(false);
			removeUpdateButton.setEnabled(false);
			return;
		} else {
			autRecognizeButton.setEnabled(true);
			autRecognizeButton.setSelection(autoRecognize);
		}
		if (pages.getSelection() == null) {
			recognizeButton.setEnabled(false);
			removeUpdateButton.setEnabled(false);
			defineModuleButton.setEnabled(false);
			autRecognizeButton.setEnabled(true);
			autRecognizeButton.setSelection(autoRecognize);
			return;
		} else {
			defineModuleButton.setEnabled(true);
			autRecognizeButton.setEnabled(true);
			autRecognizeButton.setSelection(autoRecognize);
		}

		Browser browser = needToBecheckBrowser.get(0);
		needToBecheckBrowser.remove(0);
		if (browser.isDisposed() == false
				&& browserTabs.get(browser.hashCode()).isDisposed() == false) {

			BroswerData broswerData = (BroswerData) browser
					.getData("broswerData");
			if (broswerData.getUpdatePaths().equals("null") == false) {
				browserTabs.get(browser.hashCode()).setImage(newImage);
				shell.setActive();
				shell.setSize(shell.getSize());
			} else {
				browserTabs.get(browser.hashCode()).setImage(webImage);
			}
			Object obj = browser.evaluate("return document.title;");
			if (obj != null) {
				String title = obj.toString();
				if (title.length() == 0) {
					title = "新标签页";
				}
				browserTabs.get(browser.hashCode()).setText(title);
			}
		}

		if (pages.getSelection().getControl() != browser) {
			return;
		}
		try {
			BroswerData broswerData = (BroswerData) browser
					.getData("broswerData");
			recognizeButton
					.setEnabled(broswerData.getPaths().equals("null") == false);
			defineModuleButton.setEnabled(true);
			if (broswerData.isDefineModule() == false) {
				defineModuleButton.setText("订阅模式(&D)");
			} else {
				defineModuleButton.setText("退出订阅(&B)");
			}

			removeUpdateButton.setEnabled(broswerData.getUserModuleId().equals(
					"null") == false);
			autRecognizeButton.setEnabled(true);
			autRecognizeButton.setSelection(autoRecognize);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addCssAndScript(final Browser browser, final CTabItem tabItem) {
		// browser.evaluate("document.firstChild.nodeValue=document.firstChild.nodeValue+\" \\\"http://www.w3.org/TR/html4/loose.dtd\\\"\";");
		try {
			if (browser.isDisposed() == true) {
				return;
			}
			if (browser.getUrl().equals(OlfBrowser.BLANK)) {
				return;
			}
			final BroswerData broswerData = (BroswerData) browser
					.getData("broswerData");
			if (tabItem.isDisposed() == true) {
				return;
			}

			broswerData.setUserModuleId("null");
			broswerData.setDefineModule(false);
			broswerData.setPaths("null");
			broswerData.setUpdatePaths("null");
			addJquery(browser);
			addBaseJs(browser);
			addCSS(browser);
			if (autoRecognize == true && login) { // 自动识别模式
				checkModuleExists(browser);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("加入脚本完成");
	}

	class ReFreshtThread extends Thread {
		private volatile boolean keepRun = true;

		public void run() {
			String result = null;
			for (; keepRun;) {
				try {
					Thread.sleep(checkTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}
				if (keepRun == false) {
					return;
				}
				if (display.isDisposed()) {
					return;
				}
				result = httpClient.getMethod(browserCheckUpdateUserModuleURL,
						true);
				if (result.equals("update")) {

					display.asyncExec(new Runnable() {
						public void run() {
							showNotifyDialog();
						}

					});
				}
				display.asyncExec(new Runnable() {
					public void run() {
						
						Browser tbrowser;
						Control broControl[];
						BroswerData broswerData;
						broControl = pages.getChildren();

						for (int i = 0; i < pages.getItemCount(); i++) {
							if (broControl[i] instanceof Browser) {
								tbrowser = (Browser) broControl[i];
								broswerData = (BroswerData) tbrowser
										.getData("broswerData");
								if (broswerData.getPaths().equals("null") == false) {
									if (broswerData.getLastLoadTime() + timeout < new Date()
											.getTime()) {

										tbrowser.setUrl(tbrowser.getUrl());
										
										broswerData.setCompletedStatus(BroswerData.LOADING);
									}
								}
							}
						}
					}
				});
			}
		}

		public void setKeepRun(boolean keepRun) {
			this.keepRun = keepRun;
		}

		public boolean isKeepRun() {
			return keepRun;
		}
	}

	// 提醒框
	class NotifyDialog extends Dialog {

		protected Object result;
		protected Shell inshell;

		/**
		 * Create the dialog.
		 * 
		 * @param parent
		 * @param style
		 */
		public NotifyDialog(Shell parent, int style) {
			super(parent, style);
			setText("提示");

		}

		/**
		 * Open the dialog.
		 * 
		 * @return the result
		 */
		public Object open() {
			createContents();
			inshell.open();
			inshell.layout();
			inshell.setVisible(true);
			inshell.setActive();

			Display display = getParent().getDisplay();

			while (!inshell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			notifyDialog = null;
			inshell.dispose();
			return result;
		}

		/**
		 * Create contents of the dialog.
		 */
		private void createContents() {
			inshell = new Shell(getParent(), SWT.ON_TOP | SWT.CLOSE);
			inshell.setImage((SWTResourceManager.getImage("images/logo.png")));
			inshell.setDragDetect(false);
			inshell.setAlpha(240);
			inshell.setSize(250, 140);
			inshell.setText("\u4F60\u7684\u7A7A\u95F4\u6709\u66F4\u65B0");

			Label label = new Label(inshell, SWT.NONE);
			label.setBounds(10, 10, 36, 14);
			label.setText("\u5C0A\u656C\u7684");

			Label userLabel = new Label(inshell, SWT.NONE);
			userLabel.setBounds(52, 10, 29, 14);
			userLabel.setText(userName);

			Label label_2 = new Label(inshell, SWT.NONE);
			label_2.setBounds(24, 42, 127, 14);
			label_2
					.setText("\u4F60\u6709\u65B0\u7684\u5173\u6CE8\u6D88\u606F\uFF0C\u8BF7");

			Label label_3 = new Label(inshell, SWT.NONE);
			label_3.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent e) {
					Control browsers[] = pages.getChildren();
					Browser browser;
					for (int i = 0; i < browsers.length; i++) {
						browser = (Browser) browsers[i];
						if (browser.getUrl().indexOf(base + "/user") >= 0) {
							browser.setUrl(base + "/user/myfocus");
							BroswerData broswerData = (BroswerData) browser.getData("broswerData");
							broswerData.setCompletedStatus(BroswerData.LOADING);
						
							pages.setSelection(browserTabs.get(browser
									.hashCode()));

							inshell.dispose();
							return;
						}
					}
					createABroswer(base + "/index/checklogin?userid=" + userId
							+ "&password=" + password);
					inshell.dispose();
				}
			});
			label_3.setForeground(SWTResourceManager
					.getColor(SWT.COLOR_DARK_GREEN));
			label_3.setBounds(150, 42, 29, 14);
			label_3.setText("\u70B9\u51FB");

			Label label_4 = new Label(inshell, SWT.NONE);
			label_4.setBounds(181, 42, 50, 14);
			label_4.setText("\u67E5\u770B\uFF01");
			inshell
					.setBounds(
							inshell.getMonitor().getClientArea().width - 250,
							inshell.getMonitor().getClientArea().height - 140,
							250, 140);

			Button ingore = new Button(inshell, SWT.NONE);
			ingore.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					Thread thread = new Thread() {
						public void run() {
							ignoreUpdate();
						}
					};
					thread.start();
					inshell.dispose();
				}
			});
			ingore.setBounds(86, 75, 79, 24);
			ingore.setText("\u5FFD\u7565(I)");

		}
	}

	/**
	 * 弹出警告信息框
	 * 
	 * @param message
	 */
	private void alert(String message) {
		CTabItem tabItem = pages.getSelection();
		if (tabItem == null) {
			Browser browser = createABroswer(OlfBrowser.BLANK);
			browser.execute("alert('" + message + "')");
		} else {
			Browser browser = (Browser) tabItem.getControl();
			browser.execute("alert('" + message + "')");
		}
	}
}
