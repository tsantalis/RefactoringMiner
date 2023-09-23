package eu.siacs.conversations.ui.adapter;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import eu.siacs.conversations.R;
import eu.siacs.conversations.crypto.axolotl.AxolotlService;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.entities.Contact;
import eu.siacs.conversations.entities.Conversation;
import eu.siacs.conversations.entities.DownloadableFile;
import eu.siacs.conversations.entities.Message;
import eu.siacs.conversations.entities.Message.FileParams;
import eu.siacs.conversations.entities.Transferable;
import eu.siacs.conversations.ui.ConversationActivity;
import eu.siacs.conversations.utils.GeoHelper;
import eu.siacs.conversations.utils.UIHelper;

public class MessageAdapter extends ArrayAdapter<Message> {

	private static final int SENT = 0;
	private static final int RECEIVED = 1;
	private static final int STATUS = 2;

	private ConversationActivity activity;

	private DisplayMetrics metrics;

	private OnContactPictureClicked mOnContactPictureClickedListener;
	private OnContactPictureLongClicked mOnContactPictureLongClickedListener;

	private OnLongClickListener openContextMenu = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			v.showContextMenu();
			return true;
		}
	};

	public MessageAdapter(ConversationActivity activity, List<Message> messages) {
		super(activity, 0, messages);
		this.activity = activity;
		metrics = getContext().getResources().getDisplayMetrics();
	}

	public void setOnContactPictureClicked(OnContactPictureClicked listener) {
		this.mOnContactPictureClickedListener = listener;
	}

	public void setOnContactPictureLongClicked(
			OnContactPictureLongClicked listener) {
		this.mOnContactPictureLongClickedListener = listener;
			}

	@Override
	public int getViewTypeCount() {
		return 3;
	}

	@Override
	public int getItemViewType(int position) {
		if (getItem(position).getType() == Message.TYPE_STATUS) {
			return STATUS;
		} else if (getItem(position).getStatus() <= Message.STATUS_RECEIVED) {
			return RECEIVED;
		} else {
			return SENT;
		}
	}

	private void displayStatus(ViewHolder viewHolder, Message message) {
		String filesize = null;
		String info = null;
		boolean error = false;
		if (viewHolder.indicatorReceived != null) {
			viewHolder.indicatorReceived.setVisibility(View.GONE);
		}
		boolean multiReceived = message.getConversation().getMode() == Conversation.MODE_MULTI
			&& message.getMergedStatus() <= Message.STATUS_RECEIVED;
		if (message.getType() == Message.TYPE_IMAGE || message.getType() == Message.TYPE_FILE || message.getTransferable() != null) {
			FileParams params = message.getFileParams();
			if (params.size > (1.5 * 1024 * 1024)) {
				filesize = params.size / (1024 * 1024)+ " MiB";
			} else if (params.size > 0) {
				filesize = params.size / 1024 + " KiB";
			}
			if (message.getTransferable() != null && message.getTransferable().getStatus() == Transferable.STATUS_FAILED) {
				error = true;
			}
		}
		switch (message.getMergedStatus()) {
			case Message.STATUS_WAITING:
				info = getContext().getString(R.string.waiting);
				break;
			case Message.STATUS_UNSEND:
				Transferable d = message.getTransferable();
				if (d!=null) {
					info = getContext().getString(R.string.sending_file,d.getProgress());
				} else {
					info = getContext().getString(R.string.sending);
				}
				break;
			case Message.STATUS_OFFERED:
				info = getContext().getString(R.string.offering);
				break;
			case Message.STATUS_SEND_RECEIVED:
				if (activity.indicateReceived()) {
					viewHolder.indicatorReceived.setVisibility(View.VISIBLE);
				}
				break;
			case Message.STATUS_SEND_DISPLAYED:
				if (activity.indicateReceived()) {
					viewHolder.indicatorReceived.setVisibility(View.VISIBLE);
				}
				break;
			case Message.STATUS_SEND_FAILED:
				info = getContext().getString(R.string.send_failed);
				error = true;
				break;
			default:
				if (multiReceived) {
					info = UIHelper.getMessageDisplayName(message);
				}
				break;
		}
		if (error) {
			viewHolder.time.setTextColor(activity.getWarningTextColor());
		} else {
			viewHolder.time.setTextColor(activity.getSecondaryTextColor());
		}
		if (message.getEncryption() == Message.ENCRYPTION_NONE) {
			viewHolder.indicator.setVisibility(View.GONE);
		} else {
			viewHolder.indicator.setVisibility(View.VISIBLE);
			if (message.getEncryption() == Message.ENCRYPTION_AXOLOTL) {
				AxolotlService.SQLiteAxolotlStore.Trust trust = message.getConversation()
						.getAccount().getAxolotlService().getFingerprintTrust(
								message.getAxolotlFingerprint());

				if(trust == null || trust != AxolotlService.SQLiteAxolotlStore.Trust.TRUSTED) {
					viewHolder.indicator.setColorFilter(Color.RED);
				} else {
					viewHolder.indicator.clearColorFilter();
				}
			}
		}

		String formatedTime = UIHelper.readableTimeDifferenceFull(getContext(),
				message.getMergedTimeSent());
		if (message.getStatus() <= Message.STATUS_RECEIVED) {
			if ((filesize != null) && (info != null)) {
				viewHolder.time.setText(formatedTime + " \u00B7 " + filesize +" \u00B7 " + info);
			} else if ((filesize == null) && (info != null)) {
				viewHolder.time.setText(formatedTime + " \u00B7 " + info);
			} else if ((filesize != null) && (info == null)) {
				viewHolder.time.setText(formatedTime + " \u00B7 " + filesize);
			} else {
				viewHolder.time.setText(formatedTime);
			}
		} else {
			if ((filesize != null) && (info != null)) {
				viewHolder.time.setText(filesize + " \u00B7 " + info);
			} else if ((filesize == null) && (info != null)) {
				if (error) {
					viewHolder.time.setText(info + " \u00B7 " + formatedTime);
				} else {
					viewHolder.time.setText(info);
				}
			} else if ((filesize != null) && (info == null)) {
				viewHolder.time.setText(filesize + " \u00B7 " + formatedTime);
			} else {
				viewHolder.time.setText(formatedTime);
			}
		}
	}

	private void displayInfoMessage(ViewHolder viewHolder, String text) {
		if (viewHolder.download_button != null) {
			viewHolder.download_button.setVisibility(View.GONE);
		}
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.messageBody.setVisibility(View.VISIBLE);
		viewHolder.messageBody.setText(text);
		viewHolder.messageBody.setTextColor(activity.getSecondaryTextColor());
		viewHolder.messageBody.setTypeface(null, Typeface.ITALIC);
		viewHolder.messageBody.setTextIsSelectable(false);
	}

	private void displayDecryptionFailed(ViewHolder viewHolder) {
		if (viewHolder.download_button != null) {
			viewHolder.download_button.setVisibility(View.GONE);
		}
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.messageBody.setVisibility(View.VISIBLE);
		viewHolder.messageBody.setText(getContext().getString(
				R.string.decryption_failed));
		viewHolder.messageBody.setTextColor(activity.getWarningTextColor());
		viewHolder.messageBody.setTypeface(null, Typeface.NORMAL);
		viewHolder.messageBody.setTextIsSelectable(false);
	}

	private void displayHeartMessage(final ViewHolder viewHolder, final String body) {
		if (viewHolder.download_button != null) {
			viewHolder.download_button.setVisibility(View.GONE);
		}
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.messageBody.setVisibility(View.VISIBLE);
		viewHolder.messageBody.setIncludeFontPadding(false);
		Spannable span = new SpannableString(body);
		span.setSpan(new RelativeSizeSpan(4.0f), 0, body.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		span.setSpan(new ForegroundColorSpan(activity.getWarningTextColor()), 0, body.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		viewHolder.messageBody.setText(span);
	}

	private void displayTextMessage(final ViewHolder viewHolder, final Message message) {
		if (viewHolder.download_button != null) {
			viewHolder.download_button.setVisibility(View.GONE);
		}
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.messageBody.setVisibility(View.VISIBLE);
		viewHolder.messageBody.setIncludeFontPadding(true);
		if (message.getBody() != null) {
			final String nick = UIHelper.getMessageDisplayName(message);
			final String body = message.getMergedBody().replaceAll("^" + Message.ME_COMMAND,nick + " ");
			final SpannableString formattedBody = new SpannableString(body);
			int i = body.indexOf(Message.MERGE_SEPARATOR);
			while(i >= 0) {
				final int end = i + Message.MERGE_SEPARATOR.length();
				formattedBody.setSpan(new RelativeSizeSpan(0.3f),i,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				i = body.indexOf(Message.MERGE_SEPARATOR,end);
			}
			if (message.getType() != Message.TYPE_PRIVATE) {
				if (message.hasMeCommand()) {
					final Spannable span = new SpannableString(formattedBody);
					span.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, nick.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					viewHolder.messageBody.setText(span);
				} else {
					viewHolder.messageBody.setText(formattedBody);
				}
			} else {
				String privateMarker;
				if (message.getStatus() <= Message.STATUS_RECEIVED) {
					privateMarker = activity
						.getString(R.string.private_message);
				} else {
					final String to;
					if (message.getCounterpart() != null) {
						to = message.getCounterpart().getResourcepart();
					} else {
						to = "";
					}
					privateMarker = activity.getString(R.string.private_message_to, to);
				}
				final Spannable span = new SpannableString(privateMarker + " "
						+ formattedBody);
				span.setSpan(new ForegroundColorSpan(activity
							.getSecondaryTextColor()), 0, privateMarker
						.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				span.setSpan(new StyleSpan(Typeface.BOLD), 0,
						privateMarker.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				if (message.hasMeCommand()) {
					span.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), privateMarker.length() + 1,
							privateMarker.length() + 1 + nick.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				viewHolder.messageBody.setText(span);
			}
		} else {
			viewHolder.messageBody.setText("");
		}
		viewHolder.messageBody.setTextColor(activity.getPrimaryTextColor());
		viewHolder.messageBody.setTypeface(null, Typeface.NORMAL);
		viewHolder.messageBody.setTextIsSelectable(true);
	}

	private void displayDownloadableMessage(ViewHolder viewHolder,
			final Message message, String text) {
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.messageBody.setVisibility(View.GONE);
		viewHolder.download_button.setVisibility(View.VISIBLE);
		viewHolder.download_button.setText(text);
		viewHolder.download_button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startDownloadable(message);
			}
		});
		viewHolder.download_button.setOnLongClickListener(openContextMenu);
	}

	private void displayOpenableMessage(ViewHolder viewHolder,final Message message) {
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.messageBody.setVisibility(View.GONE);
		viewHolder.download_button.setVisibility(View.VISIBLE);
		viewHolder.download_button.setText(activity.getString(R.string.open_x_file, UIHelper.getFileDescriptionString(activity, message)));
		viewHolder.download_button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				openDownloadable(message);
			}
		});
		viewHolder.download_button.setOnLongClickListener(openContextMenu);
	}

	private void displayLocationMessage(ViewHolder viewHolder, final Message message) {
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.messageBody.setVisibility(View.GONE);
		viewHolder.download_button.setVisibility(View.VISIBLE);
		viewHolder.download_button.setText(R.string.show_location);
		viewHolder.download_button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showLocation(message);
			}
		});
		viewHolder.download_button.setOnLongClickListener(openContextMenu);
	}

	private void displayImageMessage(ViewHolder viewHolder,
			final Message message) {
		if (viewHolder.download_button != null) {
			viewHolder.download_button.setVisibility(View.GONE);
		}
		viewHolder.messageBody.setVisibility(View.GONE);
		viewHolder.image.setVisibility(View.VISIBLE);
		FileParams params = message.getFileParams();
		double target = metrics.density * 288;
		int scalledW;
		int scalledH;
		if (params.width <= params.height) {
			scalledW = (int) (params.width / ((double) params.height / target));
			scalledH = (int) target;
		} else {
			scalledW = (int) target;
			scalledH = (int) (params.height / ((double) params.width / target));
		}
		viewHolder.image.setLayoutParams(new LinearLayout.LayoutParams(
				scalledW, scalledH));
		activity.loadBitmap(message, viewHolder.image);
		viewHolder.image.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(activity.xmppConnectionService
						.getFileBackend().getJingleFileUri(message), "image/*");
				getContext().startActivity(intent);
			}
		});
		viewHolder.image.setOnLongClickListener(openContextMenu);
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		final Message message = getItem(position);
		final Conversation conversation = message.getConversation();
		final Account account = conversation.getAccount();
		final int type = getItemViewType(position);
		ViewHolder viewHolder;
		if (view == null) {
			viewHolder = new ViewHolder();
			switch (type) {
				case SENT:
					view = activity.getLayoutInflater().inflate(
							R.layout.message_sent, parent, false);
					viewHolder.message_box = (LinearLayout) view
						.findViewById(R.id.message_box);
					viewHolder.contact_picture = (ImageView) view
						.findViewById(R.id.message_photo);
					viewHolder.download_button = (Button) view
						.findViewById(R.id.download_button);
					viewHolder.indicator = (ImageView) view
						.findViewById(R.id.security_indicator);
					viewHolder.image = (ImageView) view
						.findViewById(R.id.message_image);
					viewHolder.messageBody = (TextView) view
						.findViewById(R.id.message_body);
					viewHolder.time = (TextView) view
						.findViewById(R.id.message_time);
					viewHolder.indicatorReceived = (ImageView) view
						.findViewById(R.id.indicator_received);
					break;
				case RECEIVED:
					view = activity.getLayoutInflater().inflate(
							R.layout.message_received, parent, false);
					viewHolder.message_box = (LinearLayout) view
						.findViewById(R.id.message_box);
					viewHolder.contact_picture = (ImageView) view
						.findViewById(R.id.message_photo);
					viewHolder.download_button = (Button) view
						.findViewById(R.id.download_button);
					viewHolder.indicator = (ImageView) view
						.findViewById(R.id.security_indicator);
					viewHolder.image = (ImageView) view
						.findViewById(R.id.message_image);
					viewHolder.messageBody = (TextView) view
						.findViewById(R.id.message_body);
					viewHolder.time = (TextView) view
						.findViewById(R.id.message_time);
					viewHolder.indicatorReceived = (ImageView) view
						.findViewById(R.id.indicator_received);
					break;
				case STATUS:
					view = activity.getLayoutInflater().inflate(R.layout.message_status, parent, false);
					viewHolder.contact_picture = (ImageView) view.findViewById(R.id.message_photo);
					viewHolder.status_message = (TextView) view.findViewById(R.id.status_message);
					break;
				default:
					viewHolder = null;
					break;
			}
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
			if (viewHolder == null) {
				return view;
			}
		}

		if (type == STATUS) {
			if (conversation.getMode() == Conversation.MODE_SINGLE) {
				viewHolder.contact_picture.setImageBitmap(activity
						.avatarService().get(conversation.getContact(),
							activity.getPixel(32)));
				viewHolder.contact_picture.setAlpha(0.5f);
				viewHolder.status_message.setText(message.getBody());
			}
			return view;
		} else if (type == RECEIVED) {
			Contact contact = message.getContact();
			if (contact != null) {
				viewHolder.contact_picture.setImageBitmap(activity.avatarService().get(contact, activity.getPixel(48)));
			} else if (conversation.getMode() == Conversation.MODE_MULTI) {
				viewHolder.contact_picture.setImageBitmap(activity.avatarService().get(
						UIHelper.getMessageDisplayName(message),
						activity.getPixel(48)));
			}
		} else if (type == SENT) {
			viewHolder.contact_picture.setImageBitmap(activity.avatarService().get(account, activity.getPixel(48)));
		}

		viewHolder.contact_picture
			.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (MessageAdapter.this.mOnContactPictureClickedListener != null) {
						MessageAdapter.this.mOnContactPictureClickedListener
							.onContactPictureClicked(message);
					}

				}
			});
		viewHolder.contact_picture
			.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					if (MessageAdapter.this.mOnContactPictureLongClickedListener != null) {
						MessageAdapter.this.mOnContactPictureLongClickedListener
							.onContactPictureLongClicked(message);
						return true;
					} else {
						return false;
					}
				}
			});

		final Transferable transferable = message.getTransferable();
		if (transferable != null && transferable.getStatus() != Transferable.STATUS_UPLOADING) {
			if (transferable.getStatus() == Transferable.STATUS_OFFER) {
				displayDownloadableMessage(viewHolder,message,activity.getString(R.string.download_x_file, UIHelper.getFileDescriptionString(activity, message)));
			} else if (transferable.getStatus() == Transferable.STATUS_OFFER_CHECK_FILESIZE) {
				displayDownloadableMessage(viewHolder, message, activity.getString(R.string.check_x_filesize, UIHelper.getFileDescriptionString(activity, message)));
			} else {
				displayInfoMessage(viewHolder, UIHelper.getMessagePreview(activity, message).first);
			}
		} else if (message.getType() == Message.TYPE_IMAGE && message.getEncryption() != Message.ENCRYPTION_PGP && message.getEncryption() != Message.ENCRYPTION_DECRYPTION_FAILED) {
			displayImageMessage(viewHolder, message);
		} else if (message.getType() == Message.TYPE_FILE && message.getEncryption() != Message.ENCRYPTION_PGP && message.getEncryption() != Message.ENCRYPTION_DECRYPTION_FAILED) {
			if (message.getFileParams().width > 0) {
				displayImageMessage(viewHolder,message);
			} else {
				displayOpenableMessage(viewHolder, message);
			}
		} else if (message.getEncryption() == Message.ENCRYPTION_PGP) {
			if (activity.hasPgp()) {
				displayInfoMessage(viewHolder,activity.getString(R.string.encrypted_message));
			} else {
				displayInfoMessage(viewHolder,
						activity.getString(R.string.install_openkeychain));
				if (viewHolder != null) {
					viewHolder.message_box
						.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								activity.showInstallPgpDialog();
							}
						});
				}
			}
		} else if (message.getEncryption() == Message.ENCRYPTION_DECRYPTION_FAILED) {
			displayDecryptionFailed(viewHolder);
		} else {
			if (GeoHelper.isGeoUri(message.getBody())) {
				displayLocationMessage(viewHolder,message);
			} else if (message.bodyIsHeart()) {
				displayHeartMessage(viewHolder, message.getBody().trim());
			} else if (message.treatAsDownloadable() == Message.Decision.MUST) {
				displayDownloadableMessage(viewHolder, message, activity.getString(R.string.check_x_filesize, UIHelper.getFileDescriptionString(activity, message)));
			} else {
				displayTextMessage(viewHolder, message);
			}
		}

		displayStatus(viewHolder, message);

		return view;
	}

	public void startDownloadable(Message message) {
		Transferable transferable = message.getTransferable();
		if (transferable != null) {
			if (!transferable.start()) {
				Toast.makeText(activity, R.string.not_connected_try_again,
						Toast.LENGTH_SHORT).show();
			}
		} else if (message.treatAsDownloadable() != Message.Decision.NEVER) {
			activity.xmppConnectionService.getHttpConnectionManager().createNewDownloadConnection(message);
		}
	}

	public void openDownloadable(Message message) {
		DownloadableFile file = activity.xmppConnectionService.getFileBackend().getFile(message);
		if (!file.exists()) {
			Toast.makeText(activity,R.string.file_deleted,Toast.LENGTH_SHORT).show();
			return;
		}
		Intent openIntent = new Intent(Intent.ACTION_VIEW);
		openIntent.setDataAndType(Uri.fromFile(file), file.getMimeType());
		PackageManager manager = activity.getPackageManager();
		List<ResolveInfo> infos = manager.queryIntentActivities(openIntent, 0);
		if (infos.size() > 0) {
			getContext().startActivity(openIntent);
		} else {
			Toast.makeText(activity,R.string.no_application_found_to_open_file,Toast.LENGTH_SHORT).show();
		}
	}

	public void showLocation(Message message) {
		for(Intent intent : GeoHelper.createGeoIntentsFromMessage(message)) {
			if (intent.resolveActivity(getContext().getPackageManager()) != null) {
				getContext().startActivity(intent);
				return;
			}
		}
		Toast.makeText(activity,R.string.no_application_found_to_display_location,Toast.LENGTH_SHORT).show();
	}

	public interface OnContactPictureClicked {
		public void onContactPictureClicked(Message message);
	}

	public interface OnContactPictureLongClicked {
		public void onContactPictureLongClicked(Message message);
	}

	private static class ViewHolder {

		protected LinearLayout message_box;
		protected Button download_button;
		protected ImageView image;
		protected ImageView indicator;
		protected ImageView indicatorReceived;
		protected TextView time;
		protected TextView messageBody;
		protected ImageView contact_picture;
		protected TextView status_message;
	}
}
