import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.vfs.VirtualFile;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Created by ABC on 16/8/17.
 */
public class FormatYamlAction extends AnAction {

	/**
	 * Print log to "Event Log"
	 */
	private final static String GROUP_ID = "FormatYaml";
	private final static String TITLE = "FormatYaml";
	private Project mProject;

	public static void log(String msg) {
		Notification notification = new Notification(GROUP_ID, TITLE, msg, NotificationType.INFORMATION);//build a notification
		//notification.hideBalloon();//didn't work
		Notifications.Bus.notify(notification);//use the default bus to notify (application level)
		Balloon balloon = notification.getBalloon();
		if (balloon != null) {//fix: #20 潜在的NPE
			balloon.hide(true);//try to hide the balloon immediately.
		}
	}

	@Override
	public void actionPerformed(AnActionEvent event) {
		mProject = event.getData(PlatformDataKeys.PROJECT);
		format(DataKeys.VIRTUAL_FILE.getData(event.getDataContext()));

	}

	@Override
	public void update(AnActionEvent event) {
		//在Action显示之前,根据选中文件扩展名判定是否显示此Action
		VirtualFile file = DataKeys.VIRTUAL_FILE.getData(event.getDataContext());
		boolean show = file.isDirectory() || isYamlFile(file.getExtension());
		this.getTemplatePresentation().setEnabled(show);
		this.getTemplatePresentation().setVisible(show);
	}

	private boolean isYamlFile(String extension) {
		return extension.equalsIgnoreCase("yml") || extension.equalsIgnoreCase("yaml");
	}

	private void format(VirtualFile file) {
		if (file.isDirectory()) {
			for (int i = 0; i < file.getChildren().length; i++)
				format(file.getChildren()[i]);
		} else {
			if (isYamlFile(file.getExtension())) {
				log("format : " + file.getPath());
				try {
					formatYaml(file);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void formatYaml(VirtualFile file) throws Exception {
		WriteCommandAction.runWriteCommandAction(mProject, new Runnable() {
			@Override
			public void run() {
				try {
					file.setBinaryContent(toYaml(new Yaml().load(file.getInputStream())).getBytes(file.getCharset()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}


	private String toYaml(Object obj) {
		DumperOptions options = new DumperOptions();
		options.setAllowReadOnlyProperties(true);
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setIndent(4);
		return new Yaml(options).dump(obj);
	}

}