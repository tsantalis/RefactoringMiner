/**
 *
 */
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.TargetRoll;
import megamek.common.UnitType;
import megamek.common.util.EncodeControl;
import mekhq.Utilities;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.RetirementDefectionTracker;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.PersonnelTab;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.model.RetirementTableModel;
import mekhq.gui.model.UnitAssignmentTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.sorter.BonusSorter;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.RankSorter;
import mekhq.gui.sorter.WeightClassSorter;

/**
 * @author Neoancient
 *
 */
public class RetirementDefectionDialog extends JDialog {
	/**
	 *
	 */
	private static final long serialVersionUID = -5551772461081092679L;

	private static final String PAN_OVERVIEW = "PanOverview";
	private static final String PAN_RESULTS = "PanResults";

	private String currentPanel;

	private CampaignGUI hqView;
	private AtBContract contract;
	private RetirementDefectionTracker rdTracker;

	private HashMap<UUID, TargetRoll> targetRolls;
	private HashMap<UUID, UUID> unitAssignments;

	private JPanel panMain;
	private JTextArea txtInstructions;
	private CardLayout cardLayout;

	/* Overview Panel components */
	private JComboBox<String> cbGroupOverview;
	private JSpinner spnGeneralMod;
    private JLabel lblTotal;
	private RetirementTable personnelTable;
    private TableRowSorter<RetirementTableModel> personnelSorter;
    private TableRowSorter<RetirementTableModel> retireeSorter;
    private JTextArea txtTargetDetails;

    /* Results Panel components */
	private JComboBox<String> cbGroupResults;
    private JLabel lblPayment;
    private RetirementTable retireeTable;
	private JButton btnAddUnit;
	private JButton btnRemoveUnit;
	private JComboBox<String> cbUnitCategory;
	private JCheckBox chkShowAllUnits;
    private JTable unitAssignmentTable;
    private TableRowSorter<UnitAssignmentTableModel> unitSorter;

    /* Button Panel components */
	private JButton btnCancel;
	private JToggleButton btnEdit;
	private JButton btnRoll;
	private JButton btnDone;

    DecimalFormat formatter = new DecimalFormat();
	private boolean aborted = true;

	public RetirementDefectionDialog (CampaignGUI gui,
			AtBContract contract, boolean doRetirement) {
		super(gui.getFrame(), true);
		hqView = gui;
		unitAssignments = new HashMap<UUID, UUID>();
		this.contract = contract;
		rdTracker = hqView.getCampaign().getRetirementDefectionTracker();
		if (doRetirement) {
			targetRolls = rdTracker.calculateTargetNumbers(contract,
					hqView.getCampaign());
		}
		currentPanel = doRetirement?PAN_OVERVIEW:PAN_RESULTS;
        setSize(new Dimension(800, 600));
		initComponents(doRetirement);
		if (!doRetirement) {
			initResults();
			btnDone.setEnabled(unitAssignmentsComplete());
		}

	    setLocationRelativeTo(gui.getFrame());
	}

	private void initComponents(boolean doRetirement) {
		ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.RetirementDefectionDialog", new EncodeControl()); //$NON-NLS-1$
        setTitle(resourceMap.getString("title.text"));

        setLayout(new BorderLayout());
        cardLayout = new CardLayout();
        panMain = new JPanel(cardLayout);
        add(panMain, BorderLayout.CENTER);
        txtInstructions = new JTextArea();
        add(txtInstructions, BorderLayout.PAGE_START);
        txtInstructions.setEditable(false);
        txtInstructions.setWrapStyleWord(true);
        txtInstructions.setLineWrap(true);
        if (doRetirement) {
        	String instructions;
        	if (hqView.getCampaign().getCampaignOptions().getUseShareSystem()) {
        		instructions = resourceMap.getString("txtInstructions.OverviewShare.text");
        	} else {
        		instructions = resourceMap.getString("txtInstructions.Overview.text");
        	}
        	if (null == contract) {
        		instructions += "\n\nDays since last retirement roll: "
        				+ Utilities.countDaysBetween(rdTracker.getLastRetirementRoll().getTime(),
        						hqView.getCampaign().getDate());
        	}
        	txtInstructions.setText(instructions);
        } else {
            txtInstructions.setText(resourceMap.getString("txtInstructions.Results.text"));
        }
        txtInstructions.setBorder(BorderFactory.createCompoundBorder(
        		BorderFactory.createTitledBorder(resourceMap.getString("txtInstructions.title")),
        		BorderFactory.createEmptyBorder(5,5,5,5)));

        /* Overview Panel */
        if (doRetirement) {
        	JPanel panOverview = new JPanel(new BorderLayout());

        	cbGroupOverview = new JComboBox<String>();
        	for (int i = 0; i < PersonnelTab.PG_RETIRE; i++) {
        		cbGroupOverview.addItem(PersonnelTab.getPersonnelGroupName(i));
        	}
        	JPanel panTop = new JPanel();
        	panTop.setLayout(new BoxLayout(panTop, BoxLayout.X_AXIS));
        	panTop.add(cbGroupOverview);
        	panTop.add(Box.createHorizontalGlue());

        	JLabel lblGeneralMod = new JLabel(resourceMap.getString("lblGeneralMod.text"));
        	spnGeneralMod = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        	spnGeneralMod.setToolTipText(resourceMap.getString("spnGeneralMod.toolTipText"));
        	if (hqView.getCampaign().getCampaignOptions().getCustomRetirementMods()) {
        		panTop.add(lblGeneralMod);
        		panTop.add(spnGeneralMod);
        		spnGeneralMod.addChangeListener(new ChangeListener() {
        			@Override
        			public void stateChanged(ChangeEvent arg0) {
        				personnelTable.setGeneralMod((Integer)spnGeneralMod.getValue());
        			}
        		});
        	}

        	JLabel lblTotalDesc = new JLabel();
        	lblTotal = new JLabel();
        	lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        	if (hqView.getCampaign().getCampaignOptions().getUseShareSystem()) {
        		lblTotalDesc.setText(resourceMap.getString("lblTotalShares.text"));
            	lblTotal.setText(Integer.toString(getTotalShares()));
        	} else {
        		lblTotalDesc.setText(resourceMap.getString("lblTotalBonus.text"));
            	lblTotal.setText("0");
        	}
        	panTop.add(lblTotalDesc);
        	panTop.add(Box.createRigidArea(new Dimension(5, 0)));
        	panTop.add(lblTotal);
        	panOverview.add(panTop, BorderLayout.PAGE_START);

        	RetirementTableModel model = new RetirementTableModel(hqView.getCampaign());
        	personnelTable = new RetirementTable(model, hqView);

        	personnelSorter = new TableRowSorter<RetirementTableModel>(model);
            personnelSorter.setComparator(RetirementTableModel.COL_PERSON, new RankSorter(hqView.getCampaign()));
            personnelSorter.setComparator(RetirementTableModel.COL_PAYOUT, new FormattedNumberSorter());
            personnelSorter.setComparator(RetirementTableModel.COL_BONUS_COST, new BonusSorter());
            personnelSorter.setComparator(RetirementTableModel.COL_PAY_BONUS, new BonusSorter());
            personnelSorter.setComparator(RetirementTableModel.COL_SHARES, new FormattedNumberSorter());
        	personnelTable.setRowSorter(personnelSorter);
        	ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        	sortKeys.add(new RowSorter.SortKey(PersonnelTableModel.COL_RANK, SortOrder.DESCENDING));
        	personnelSorter.setSortKeys(sortKeys);

        	cbGroupOverview.addActionListener(new ActionListener() {
        		@Override
                public void actionPerformed(ActionEvent evt) {
        			filterPersonnel(personnelSorter, cbGroupOverview.getSelectedIndex(), false);
        		}
        	});

        	personnelTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
        		@Override
                public void valueChanged(ListSelectionEvent ev) {
        			if (personnelTable.getSelectedRow() <= 0) {
        				return;
        			}
        			int row = personnelTable.convertRowIndexToModel(personnelTable.getSelectedRow());
        			UUID id = ((RetirementTableModel)(personnelTable.getModel())).getPerson(row).getId();
        			txtTargetDetails.setText(targetRolls.get(id).getDesc() +
        					(payBonus(id)?" -1 (Bonus)":"") +
        					((miscModifier(id) != 0)?miscModifier(id) + " (Misc)":""));
        		}
        	});

        	personnelTable.getColumnModel().getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_PAY_BONUS)).
        	setCellEditor(new DefaultCellEditor(new JCheckBox()));
        	XTableColumnModel columnModel = (XTableColumnModel)personnelTable.getColumnModel();
        	columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_PAYOUT)), false);
        	columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_UNIT)), false);
        	columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_RECRUIT)), false);
        	if (hqView.getCampaign().getCampaignOptions().getUseShareSystem()) {
        		columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_BONUS_COST)), false);
        		columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_PAY_BONUS)), false);
        	} else {
        		columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_SHARES)), false);
        	}
        	columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_MISC_MOD)),
        			hqView.getCampaign().getCampaignOptions().getCustomRetirementMods());
            model.setData(targetRolls);
            model.addTableModelListener(new TableModelListener() {
        		@Override
        		public void tableChanged(TableModelEvent ev) {
        			if (!hqView.getCampaign().getCampaignOptions().getUseShareSystem()) {
        			    long bonus = getTotalBonus();
        			    if (bonus > hqView.getCampaign().getFinances().getBalance()) {
        			        lblTotal.setText("<html><font color='red'>"
        			                + formatter.format(getTotalBonus()) + "</font></html>");
        			        btnRoll.setEnabled(false);
        			    } else {
        			        lblTotal.setText(formatter.format(getTotalBonus()));
        			        btnRoll.setEnabled(true);
        			    }
        			}
        		}
        	});
 
        	JScrollPane scroll = new JScrollPane();
        	scroll.setViewportView(personnelTable);
        	scroll.setPreferredSize(new Dimension(500, 500));
        	panOverview.add(scroll, BorderLayout.CENTER);

        	txtTargetDetails = new JTextArea();
        	panOverview.add(txtTargetDetails, BorderLayout.PAGE_END);

        	panMain.add(panOverview, PAN_OVERVIEW);
		}

        /* Results Panel */

        JPanel panRetirees = new JPanel(new BorderLayout());

		cbGroupResults = new JComboBox<String>();
		cbGroupResults.addItem("All Personnel");
        for (int i = 1; i < PersonnelTab.PG_NUM; i++) {
        	cbGroupResults.addItem(PersonnelTab.getPersonnelGroupName(i));
        }
        JPanel panTop = new JPanel();
        panTop.setLayout(new BoxLayout(panTop, BoxLayout.X_AXIS));
        panTop.add(cbGroupResults);
        panTop.add(Box.createHorizontalGlue());

        JLabel lblFinalPayout = new JLabel(resourceMap.getString("lblFinalPayout.text"));
        lblPayment = new JLabel();
        lblPayment.setHorizontalAlignment(SwingConstants.RIGHT);
        panTop.add(lblFinalPayout);
        panTop.add(Box.createRigidArea(new Dimension(5, 0)));
        panTop.add(lblPayment);
        cbUnitCategory = new JComboBox<String>();
        cbUnitCategory.addItem("All Units");
        for (int i = 0; i < UnitType.SIZE; i++) {
            cbUnitCategory.addItem(UnitType.getTypeDisplayableName(i));
        }
        cbUnitCategory.setSelectedIndex(0);
        cbUnitCategory.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                filterUnits();
            }
        });
        panTop.add(cbUnitCategory);
        chkShowAllUnits = new JCheckBox("Show All Units");
        chkShowAllUnits.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
            	if (chkShowAllUnits.isSelected()) {
            		cbUnitCategory.setSelectedIndex(0);
            	} else {
            		setUnitGroup();
            	}
                filterUnits();
            }
        });
        panTop.add(Box.createHorizontalGlue());
        panTop.add(chkShowAllUnits);

        panRetirees.add(panTop, BorderLayout.PAGE_START);

		RetirementTableModel model = new RetirementTableModel(hqView.getCampaign());
		retireeTable = new RetirementTable(model, hqView);
        retireeSorter = new TableRowSorter<RetirementTableModel>(model);
        retireeSorter.setComparator(RetirementTableModel.COL_PERSON, new RankSorter(hqView.getCampaign()));
        retireeTable.setRowSorter(retireeSorter);
    	ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
    	sortKeys.add(new RowSorter.SortKey(PersonnelTableModel.COL_RANK, SortOrder.DESCENDING));
        retireeSorter.setSortKeys(sortKeys);
        cbGroupResults.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                filterPersonnel(retireeSorter, cbGroupResults.getSelectedIndex(), true);
            }
        });

        retireeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
            public void valueChanged(ListSelectionEvent ev) {
				enableAddRemoveButtons();
				setUnitGroup();
			}
        });
        model.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent arg0) {
				lblPayment.setText(formatter.format(totalPayout()));
			}
        });

        XTableColumnModel columnModel = (XTableColumnModel)retireeTable.getColumnModel();
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_ASSIGN)), false);
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_FORCE)), false);
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_TARGET)), false);
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_BONUS_COST)), false);
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_PAY_BONUS)), false);
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_MISC_MOD)), false);
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_SHARES)), false);


        UnitAssignmentTableModel unitModel = new UnitAssignmentTableModel(hqView.getCampaign());
        unitAssignmentTable = new JTable(unitModel);
        unitAssignmentTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        unitAssignmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        columnModel = new XTableColumnModel();
        unitAssignmentTable.setColumnModel(columnModel);
        unitAssignmentTable.createDefaultColumnsFromModel();
        unitSorter = new TableRowSorter<UnitAssignmentTableModel>(unitModel);
        unitSorter.setComparator(UnitAssignmentTableModel.COL_UNIT, new WeightClassSorter());
        unitAssignmentTable.setRowSorter(unitSorter);
        ArrayList<RowSorter.SortKey> unitSortKeys = new ArrayList<RowSorter.SortKey>();
        unitSortKeys.add(new RowSorter.SortKey(UnitAssignmentTableModel.COL_UNIT, SortOrder.DESCENDING));        sortKeys.add(new RowSorter.SortKey(UnitAssignmentTableModel.COL_UNIT, SortOrder.DESCENDING));
        unitSorter.setSortKeys(unitSortKeys);
        TableColumn column = null;
        for (int i = 0; i < UnitAssignmentTableModel.N_COL; i++) {
            column = unitAssignmentTable.getColumnModel().getColumn(unitAssignmentTable.convertColumnIndexToView(i));
            column.setPreferredWidth(model.getColumnWidth(i));
            	column.setCellRenderer(unitModel.getRenderer(i, hqView.getIconPackage()));
        }

        unitAssignmentTable.setRowHeight(80);
        unitAssignmentTable.setIntercellSpacing(new Dimension(0, 0));
        unitAssignmentTable.setShowGrid(false);
        unitAssignmentTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
            public void valueChanged(ListSelectionEvent ev) {
				enableAddRemoveButtons();
			}
        });

        JPanel panResults = new JPanel();
        panResults.setLayout(new BoxLayout(panResults, BoxLayout.X_AXIS));
        JScrollPane scroll = new JScrollPane();
		scroll.setViewportView(retireeTable);
		panResults.add(scroll);
        JPanel panAddRemoveBtns = new JPanel();
        panAddRemoveBtns.setLayout(new BoxLayout(panAddRemoveBtns, BoxLayout.Y_AXIS));
        btnAddUnit = new JButton("<<<");
        btnAddUnit.setEnabled(false);
        btnAddUnit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				addUnit();
			}
        });
        panAddRemoveBtns.add(btnAddUnit);
        btnRemoveUnit = new JButton(">>>");
        btnRemoveUnit.setEnabled(false);
        btnRemoveUnit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				removeUnit();
			}
        });
        panAddRemoveBtns.add(btnRemoveUnit);
		panResults.add(panAddRemoveBtns);

        scroll = new JScrollPane();
		scroll.setViewportView(unitAssignmentTable);
		panResults.add(scroll);

        panRetirees.add(panResults, BorderLayout.CENTER);
        panMain.add(panRetirees, PAN_RESULTS);

        cardLayout.show(panMain, currentPanel);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
		btnCancel = new JButton(resourceMap.getString("btnCancel.text"));
		btnCancel.addActionListener(buttonListener);
		btnEdit = new JToggleButton(resourceMap.getString("btnEdit.text"));
		btnEdit.addActionListener(buttonListener);
		btnEdit.setVisible(currentPanel.equals(PAN_RESULTS));
		btnEdit.setEnabled(hqView.getCampaign().isGM());
		btnEdit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				btnDone.setEnabled(btnEdit.isSelected() || unitAssignmentsComplete());
				((RetirementTableModel)retireeTable.getModel()).setEditPayout(btnEdit.isSelected());
			}
		});
		btnRoll = new JButton(resourceMap.getString("btnRoll.text"));
		btnRoll.addActionListener(buttonListener);
		btnDone = new JButton(resourceMap.getString("btnDone.text"));
		btnDone.addActionListener(buttonListener);
		btnPanel.add(btnCancel);
		btnPanel.add(btnEdit);
		btnPanel.add(btnRoll);
		btnPanel.add(btnDone);
		btnRoll.setVisible(doRetirement);
		btnDone.setVisible(!doRetirement);
		add(btnPanel, BorderLayout.PAGE_END);
	}

	public ActionListener buttonListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (ev.getSource().equals(btnRoll)) {
				for (UUID id : targetRolls.keySet()) {
					if (payBonus(id)) {
						targetRolls.get(id).addModifier(-1, "Bonus");
					}
					if (miscModifier(id) != 0) {
						targetRolls.get(id).addModifier(miscModifier(id), "Misc");
					}
				}
				rdTracker.rollRetirement(contract, targetRolls,
						RetirementDefectionTracker.getShareValue(hqView.getCampaign()),
								hqView.getCampaign());
				initResults();

				ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.RetirementDefectionDialog", new EncodeControl()); //$NON-NLS-1$
				btnEdit.setVisible(true);
				btnRoll.setVisible(false);
				btnDone.setVisible(true);
				btnDone.setEnabled(unitAssignmentsComplete());

				currentPanel = PAN_RESULTS;
				cardLayout.show(panMain, currentPanel);
				if (hqView.getCampaign().getCampaignOptions().getUseShareSystem()) {
					txtInstructions.setText(resourceMap.getString("txtInstructions.ResultsShare.text"));
				} else {
					txtInstructions.setText(resourceMap.getString("txtInstructions.Results.text"));
				}
                if (getTotalBonus() > 0) {
                    hqView.getCampaign().getFinances().debit(getTotalBonus(), Transaction.C_SALARY, "Bonus Payments",
                            hqView.getCampaign().getDate());
                }
			} else if (ev.getSource().equals(btnDone)) {
				for (UUID pid : ((RetirementTableModel)retireeTable.getModel()).getAltPayout().keySet()) {
					rdTracker.getPayout(pid).setCbills(((RetirementTableModel)retireeTable.getModel()).getAltPayout().get(pid));
				}
				aborted = false;
				setVisible(false);
			} else if (ev.getSource().equals(btnCancel)){
				aborted = true;
				setVisible(false);
			}
		}
	};


	private void initResults() {
		/* Find unassigned units that can be stolen */
		ArrayList<UUID> unassignedMechs = new ArrayList<UUID>();
		ArrayList<UUID> unassignedASF = new ArrayList<UUID>();
		ArrayList<UUID> availableUnits = new ArrayList<UUID>();
		for (Unit u : hqView.getCampaign().getUnits()) {
			if (!u.isAvailable()) {
				continue;
			}
			availableUnits.add(u.getId());
			if (UnitType.MEK == UnitType.determineUnitTypeCode(u.getEntity())) {
				if (null == u.getCommander()) {
					unassignedMechs.add(u.getId());
				}
			}
			if (UnitType.AERO == UnitType.determineUnitTypeCode(u.getEntity())) {
				if (null == u.getCommander()) {
					unassignedASF.add(u.getId());
				}
			}
		}
		/* Defectors who steal a unit will take either the one they were
		 * piloting or one of the unassigned units (50/50, unless there
		 * is only one choice)
		 */
		for (UUID id : rdTracker.getRetirees(contract)) {
			Person p = hqView.getCampaign().getPerson(id);
			if (rdTracker.getPayout(id).hasStolenUnit()) {
				boolean unassignedAvailable = (unassignedMechs.size() > 0 &&
						p.getPrimaryRole() == Person.T_MECHWARRIOR) ||
						(unassignedASF.size() > 0 &&
								p.getPrimaryRole() == Person.T_AERO_PILOT);
				/* If a unit has previously been assigned, check that it is still available
				 * and either assigned to the current player or unassigned. If so, keep
				 * the previous value.
				 */
				if (null != rdTracker.getPayout(id).getStolenUnitId() &&
						null != hqView.getCampaign().getUnit(rdTracker.getPayout(id).getStolenUnitId()) &&
						(null == hqView.getCampaign().getUnit(rdTracker.getPayout(id).getStolenUnitId()).getCommander() ||
								p.getId() == hqView.getCampaign().getUnit(rdTracker.getPayout(id).getStolenUnitId()).getCommander().getId())) {
					continue;
				}
				if (null != hqView.getCampaign().getPerson(id).getUnitId() &&
						(Compute.d6() < 4 || !unassignedAvailable)) {
					unitAssignments.put(id, p.getUnitId());
				} else if (unassignedAvailable) {
					if (p.getPrimaryRole() == Person.T_MECHWARRIOR) {
						int roll = Compute.randomInt(unassignedMechs.size());
						unitAssignments.put(id, unassignedMechs.get(roll));
						rdTracker.getPayout(id).setStolenUnitId(unassignedMechs.get(roll));
						availableUnits.remove(unassignedMechs.get(roll));
						unassignedMechs.remove(roll);
					}
					if (p.getPrimaryRole() == Person.T_AERO_PILOT) {
						int roll = Compute.randomInt(unassignedASF.size());
						unitAssignments.put(id, unassignedASF.get(roll));
						rdTracker.getPayout(id).setStolenUnitId(unassignedASF.get(roll));
						availableUnits.remove(unassignedASF.get(roll));
						unassignedASF.remove(roll);
					}
				}
			}
			/* Retirees who brought a unit will take the same unit when
			 * they go if it is still around and has not been stolen.
			 */
			if (hqView.getCampaign().getCampaignOptions().getTrackOriginalUnit() &&
					null != p.getOriginalUnitId() &&
					!unitAssignments.values().contains(p.getOriginalUnitId()) &&
					null != hqView.getCampaign().getUnit(p.getOriginalUnitId())) {
				unitAssignments.put(id, p.getOriginalUnitId());
				if (hqView.getCampaign().getCampaignOptions().getUseShareSystem()) {
					rdTracker.
					getPayout(id).setCbills(Math.max(0,
							rdTracker.getPayout(id).getCbills() -
							hqView.getCampaign().getUnit(p.getOriginalUnitId()).getBuyCost()));
				}
			}
			/* For infantry, the unit commander makes a retirement roll on behalf of the
			 * entire unit. Unassigned infantry can retire individually.
			 */
			if (p.getUnitId() != null && (p.getPrimaryRole() == Person.T_INFANTRY ||
					p.getPrimaryRole() == Person.T_BA)) {
				unitAssignments.put(id, p.getUnitId());
			}
			((UnitAssignmentTableModel)unitAssignmentTable.getModel()).setData(availableUnits);
		}

		ArrayList<UUID> retireeList = new ArrayList<UUID>();
		boolean showRecruitColumn = false;
		for (UUID pid : rdTracker.getRetirees(contract)) {
			retireeList.add(pid);
			if (hqView.getCampaign().getRetirementDefectionTracker().getPayout(pid).getDependents() > 0 ||
					hqView.getCampaign().getRetirementDefectionTracker().getPayout(pid).hasHeir() ||
					hqView.getCampaign().getRetirementDefectionTracker().getPayout(pid).hasRecruit()) {
				showRecruitColumn = true;
			}
		}
        ((XTableColumnModel)retireeTable.getColumnModel()).setColumnVisible(retireeTable.getColumnModel().getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_RECRUIT)), !showRecruitColumn);
		((RetirementTableModel)retireeTable.getModel()).setData(retireeList, unitAssignments);
		filterPersonnel(retireeSorter, cbGroupResults.getSelectedIndex(), true);
		lblPayment.setText(formatter.format(totalPayout()));
	}

	private void filterPersonnel(TableRowSorter<RetirementTableModel> sorter, int groupIndex, boolean resultsView) {
        final int nGroup = groupIndex;
        final boolean results = resultsView;
        RowFilter<RetirementTableModel, Integer> personTypeFilter = new RowFilter<RetirementTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends RetirementTableModel, ? extends Integer> entry) {
                RetirementTableModel personModel = entry.getModel();
                Person person = personModel.getPerson(entry.getIdentifier());
                if (results && null != rdTracker.getRetirees(contract) &&
                		!rdTracker.getRetirees(contract).contains(person.getId())) {
                	return false;
                }
                int type = person.getPrimaryRole();
                if ((nGroup == 0) ||
                    (nGroup == PersonnelTab.PG_COMBAT && type <= Person.T_SPACE_GUNNER) ||
                    (nGroup == PersonnelTab.PG_SUPPORT && type > Person.T_SPACE_GUNNER) ||
                    (nGroup == PersonnelTab.PG_MW && type == Person.T_MECHWARRIOR) ||
                    (nGroup == PersonnelTab.PG_CREW && (type == Person.T_GVEE_DRIVER || type == Person.T_NVEE_DRIVER || type == Person.T_VTOL_PILOT || type == Person.T_VEE_GUNNER)) ||
                    (nGroup == PersonnelTab.PG_PILOT && type == Person.T_AERO_PILOT) ||
                    (nGroup == PersonnelTab.PG_CPILOT && type == Person.T_CONV_PILOT) ||
                    (nGroup == PersonnelTab.PG_PROTO && type == Person.T_PROTO_PILOT) ||
                    (nGroup == PersonnelTab.PG_BA && type == Person.T_BA) ||
                    (nGroup == PersonnelTab.PG_SOLDIER && type == Person.T_INFANTRY) ||
                    (nGroup == PersonnelTab.PG_VESSEL && (type == Person.T_SPACE_PILOT || type == Person.T_SPACE_CREW || type == Person.T_SPACE_GUNNER || type == Person.T_NAVIGATOR)) ||
                    (nGroup == PersonnelTab.PG_TECH && type >= Person.T_MECH_TECH && type < Person.T_DOCTOR) ||
                    (nGroup == PersonnelTab.PG_DOC && ((type == Person.T_DOCTOR) || (type == Person.T_MEDIC))) ||
                    (nGroup == PersonnelTab.PG_ADMIN && type > Person.T_MEDIC)
                        ) {
                    return person.isActive() || results;
                }
                if ((nGroup == PersonnelTab.PG_MIA && person.getStatus() == Person.S_MIA) ||
                		(nGroup == PersonnelTab.PG_KIA && person.getStatus() == Person.S_KIA) ||
                		(nGroup == PersonnelTab.PG_RETIRE && person.getStatus() == Person.S_RETIRED)) {
                	return true;
                }
                return false;
            }
        };
        sorter.setRowFilter(personTypeFilter);
	}

    public void filterUnits() {
        RowFilter<UnitAssignmentTableModel, Integer> unitTypeFilter = null;
        final int nGroup = cbUnitCategory.getSelectedIndex() - 1;
        unitTypeFilter = new RowFilter<UnitAssignmentTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends UnitAssignmentTableModel, ? extends Integer> entry) {
                UnitAssignmentTableModel unitModel = entry.getModel();
                Unit unit = unitModel.getUnit(entry.getIdentifier());
                if (!chkShowAllUnits.isSelected() &&
            			retireeTable.getSelectedRow() >= 0) {
            		Person selectedPerson = ((RetirementTableModel)retireeTable.getModel()).
            				getPerson(retireeTable.convertRowIndexToModel(retireeTable.getSelectedRow()));
            		if (null != rdTracker.getPayout(selectedPerson.getId()) &&
            				rdTracker.getPayout(selectedPerson.getId()).getWeightClass() > 0 &&
            				weightClassIndex(unit) != rdTracker.getPayout(selectedPerson.getId()).getWeightClass()) {
            			return false;
            		}
            	}
                /* Can't really give a platoon as payment */
                if (UnitType.determineUnitTypeCode(unit.getEntity()) == UnitType.BATTLE_ARMOR ||
                		UnitType.determineUnitTypeCode(unit.getEntity()) == UnitType.INFANTRY) {
                	return false;
                }
                if (unitAssignments.values().contains(unit.getId())) {
                	return false;
                }
                if (nGroup < 0) {
                    return true;
                }
                Entity en = unit.getEntity();
                int type = -1;
                if (null != en) {
                    type = UnitType.determineUnitTypeCode(en);
                }
                return type == nGroup;
            }
        };
        unitSorter.setRowFilter(unitTypeFilter);
    }

	public static int weightClassIndex(Unit u) {
		int retVal = u.getEntity().getWeightClass();
		if (u.getEntity().isClan() || u.getEntity().getTechLevel() > megamek.common.TechConstants.T_INTRO_BOXSET) {
			retVal++;
		}
		if (!u.isFunctional()) {
			retVal--;
		}
		return Math.max(0, retVal);
	}

	public long totalPayout() {
		if (null == rdTracker.getRetirees(contract)) {
			return 0;
		}
		long retVal = 0;
		for (UUID id : rdTracker.getRetirees(contract)) {
			if (null == rdTracker.getPayout(id)) {
				continue;
			}
			if (((RetirementTableModel)retireeTable.getModel()).getAltPayout().keySet().contains(id)) {
				retVal += ((RetirementTableModel)retireeTable.getModel()).getAltPayout().get(id);
				continue;
			}
			long payout = rdTracker.getPayout(id).getCbills();
			/* If no unit is required as part of the payout, the unit is part or all of the
			 * final payout.
			 */
			if ((rdTracker.getPayout(id).getWeightClass() == 0 &&
					null != unitAssignments.get(id) &&
							null != hqView.getCampaign().getUnit(unitAssignments.get(id)))) {
				payout -= hqView.getCampaign().getUnit(unitAssignments.get(id)).getBuyCost();
			} else if ((hqView.getCampaign().getCampaignOptions().getUseShareSystem() &&
							hqView.getCampaign().getCampaignOptions().getTrackOriginalUnit() &&
							hqView.getCampaign().getPerson(id).getOriginalUnitId() == unitAssignments.get(id)) &&
					null != hqView.getCampaign().getUnit(unitAssignments.get(id))) {
				payout -= hqView.getCampaign().getUnit(unitAssignments.get(id)).getBuyCost();
			}
			/*  If using the share system and tracking the original unit,
			 * the payout is also reduced by the value of the unit.
			 */
			if (hqView.getCampaign().getCampaignOptions().getUseShareSystem() &&
					hqView.getCampaign().getCampaignOptions().getTrackOriginalUnit() &&
					hqView.getCampaign().getPerson(id).getOriginalUnitId() == unitAssignments.get(id) &&
					null != hqView.getCampaign().getUnit(unitAssignments.get(id))) {
				payout -= hqView.getCampaign().getUnit(unitAssignments.get(id)).getBuyCost();
			}
			/* If the unit given in payment is of lower quality than required, pay
			 * an additional 3M C-bills per class.
			 */
			if (null != unitAssignments.get(id)) {
				payout += getShortfallAdjustment(rdTracker.getPayout(id).getWeightClass(),
						RetirementDefectionDialog.weightClassIndex(hqView.getCampaign().getUnit(unitAssignments.get(id))));
			}
			/* If the pilot has stolen a unit, there is no payout */
			if (rdTracker.getPayout(id).hasStolenUnit() &&
					null != unitAssignments.get(id)) {
				payout = 0;
			}
			retVal += Math.max(0, payout);
		}
		return retVal;
	}

	public boolean payBonus(UUID id) {
		return ((RetirementTableModel)personnelTable.getModel()).getPayBonus(id);
	}

	public int miscModifier(UUID id) {
		return ((RetirementTableModel)personnelTable.getModel()).getMiscModifier(id) +
				(Integer)spnGeneralMod.getValue();
	}

	private int getTotalShares() {
		int retVal = 0;
		for (UUID id : targetRolls.keySet()) {
			retVal += hqView.getCampaign().getPerson(id).getNumShares(hqView.getCampaign().getCampaignOptions().getSharesForAll());
		}
		return retVal;
	}

	private long getTotalBonus() {
		long retVal = 0;
		for (UUID id : targetRolls.keySet()) {
			if (((RetirementTableModel)(personnelTable.getModel())).getPayBonus(id)) {
				retVal += RetirementDefectionTracker.getBonusCost(hqView.getCampaign().getPerson(id));
			}
		}
		return retVal;
	}

	/* It is possible that there may not be enough units of the required
	 * weight/tech level for all retirees. This is not address by the AtB
	 * rules, so I have improvised by allowing smaller units to be given,
	 * but at a penalty of 3,000,000 C-bills per class difference (based
	 * on the values given in IOps Beta/Creating a Force).
	 */
	public static long getShortfallAdjustment(int required, int actual) {
		if (actual >= required) {
			return 0;
		}
		return (required - actual) * 3000000;
	}

	public UUID getUnitId(UUID pid) {
		return unitAssignments.get(pid);
	}

	public HashMap<UUID, UUID> getUnitAssignments() {
		return unitAssignments;
	}

	public boolean wasAborted() {
		return aborted;
	}

	private boolean unitAssignmentsComplete() {
		for (UUID id : rdTracker.getRetirees(contract)) {
			if (rdTracker.getPayout(id).getWeightClass() > 0 &&
					!unitAssignments.keySet().contains(id)) {
				return false;
			}
		}
		return true;
	}

	private void enableAddRemoveButtons() {
		if (retireeTable.getSelectedRow() < 0) {
			btnAddUnit.setEnabled(false);
			btnRemoveUnit.setEnabled(false);
		} else {
			int retireeRow = retireeTable.convertRowIndexToModel(retireeTable.getSelectedRow());
			UUID pid = ((RetirementTableModel)(retireeTable.getModel())).getPerson(retireeRow).getId();
			if (null != rdTracker.getPayout(pid) &&
					rdTracker.getPayout(pid).hasStolenUnit() &&
					!btnEdit.isSelected()) {
				btnAddUnit.setEnabled(false);
				btnRemoveUnit.setEnabled(false);
			} else if (hqView.getCampaign().getPerson(pid).getPrimaryRole() == Person.T_INFANTRY ||
					hqView.getCampaign().getPerson(pid).getPrimaryRole() == Person.T_BA) {
				btnAddUnit.setEnabled(false);
				btnRemoveUnit.setEnabled(false);
			} else if (unitAssignments.keySet().contains(pid)) {
				btnAddUnit.setEnabled(false);
				if ((hqView.getCampaign().getCampaignOptions().getTrackOriginalUnit() &&
						unitAssignments.get(pid) == hqView.getCampaign().getPerson(pid).getOriginalUnitId()) &&
						!btnEdit.isSelected()) {
					btnRemoveUnit.setEnabled(false);
				} else {
					btnRemoveUnit.setEnabled(true);
				}
			} else if (null != rdTracker.getPayout(pid) &&
					rdTracker.getPayout(pid).getWeightClass() > 0) {
				if (unitAssignmentTable.getSelectedRow() < 0) {
					btnAddUnit.setEnabled(false);
				} else if (btnEdit.isSelected()) {
					btnAddUnit.setEnabled(true);
				} else {
					Unit unit = ((UnitAssignmentTableModel)unitAssignmentTable.getModel()).getUnit(unitAssignmentTable.convertRowIndexToModel(unitAssignmentTable.getSelectedRow()));
					btnAddUnit.setEnabled(hqView.getCampaign().getPerson(pid).canDrive(unit.getEntity()));
				}
				btnRemoveUnit.setEnabled(false);
			} else {
				btnAddUnit.setEnabled(unitAssignmentTable.getSelectedRow() >= 0);
				btnRemoveUnit.setEnabled(false);
			}
		}
	}

	private void addUnit() {
		Person person = ((RetirementTableModel)retireeTable.getModel()).getPerson(retireeTable.convertRowIndexToModel(retireeTable.getSelectedRow()));
		Unit unit = ((UnitAssignmentTableModel)unitAssignmentTable.getModel()).getUnit(unitAssignmentTable.convertRowIndexToModel(unitAssignmentTable.getSelectedRow()));
		unitAssignments.put(person.getId(), unit.getId());
		btnDone.setEnabled(btnEdit.isSelected() || unitAssignmentsComplete());
		((RetirementTableModel)retireeTable.getModel()).fireTableDataChanged();
		filterUnits();
	}

	private void removeUnit() {
		Person person = ((RetirementTableModel)retireeTable.getModel()).getPerson(retireeTable.convertRowIndexToModel(retireeTable.getSelectedRow()));
		unitAssignments.remove(person.getId());
		btnDone.setEnabled(btnEdit.isSelected() || unitAssignmentsComplete());
		((RetirementTableModel)retireeTable.getModel()).fireTableDataChanged();
		filterUnits();
	}

	private void setUnitGroup() {
		if (!chkShowAllUnits.isSelected() && retireeTable.getSelectedRow() >= 0) {
			Person p = ((RetirementTableModel)retireeTable.getModel()).getPerson(retireeTable.convertRowIndexToModel(retireeTable.getSelectedRow()));
			switch (p.getPrimaryRole()) {
			case Person.T_MECHWARRIOR:
				cbUnitCategory.setSelectedIndex(UnitType.MEK + 1);
				break;
			case Person.T_GVEE_DRIVER:
			case Person.T_VEE_GUNNER:
				cbUnitCategory.setSelectedIndex(UnitType.TANK + 1);
				break;
			case Person.T_BA:
				cbUnitCategory.setSelectedIndex(UnitType.BATTLE_ARMOR + 1);
				break;
			case Person.T_INFANTRY:
				cbUnitCategory.setSelectedIndex(UnitType.INFANTRY + 1);
				break;
			case Person.T_PROTO_PILOT:
				cbUnitCategory.setSelectedIndex(UnitType.PROTOMEK + 1);
				break;
			case Person.T_VTOL_PILOT:
				cbUnitCategory.setSelectedIndex(UnitType.VTOL + 1);
				break;
			case Person.T_NVEE_DRIVER:
				cbUnitCategory.setSelectedIndex(UnitType.NAVAL + 1);
				break;
			case Person.T_CONV_PILOT:
				cbUnitCategory.setSelectedIndex(UnitType.CONV_FIGHTER + 1);
				break;
			case Person.T_AERO_PILOT:
				cbUnitCategory.setSelectedIndex(UnitType.AERO + 1);
				break;
			default:
				cbUnitCategory.setSelectedIndex(0);
			}
			filterUnits();
		}
	}
}

class RetirementTable extends JTable {

	/**
	 *
	 */
	private static final long serialVersionUID = -2839165270046226216L;

	private class SpinnerEditor extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = 7956499745127048276L;
		private JSpinner spinner;

		public SpinnerEditor() {
			spinner = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
			((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setEditable(false);
		}

		@Override
		public Object getCellEditorValue() {
			return spinner.getValue();
		}

		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {
			spinner.setValue(value);
			return spinner;
		}
	}

	public RetirementTable(RetirementTableModel model, CampaignGUI hqView) {
		super(model);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        XTableColumnModel columnModel = new XTableColumnModel();
        setColumnModel(columnModel);
        createDefaultColumnsFromModel();
        TableColumn column = null;
        for (int i = 0; i < RetirementTableModel.N_COL; i++) {
            column = getColumnModel().getColumn(convertColumnIndexToView(i));
            column.setPreferredWidth(model.getColumnWidth(i));
            if (i != RetirementTableModel.COL_PAY_BONUS &&
            		i != RetirementTableModel.COL_MISC_MOD) {
            	column.setCellRenderer(model.getRenderer(i, hqView.getIconPackage()));
            }
        }

        setRowHeight(80);
        setIntercellSpacing(new Dimension(0, 0));
        setShowGrid(false);

        getColumnModel().getColumn(convertColumnIndexToView(RetirementTableModel.COL_PAY_BONUS)).
			setCellEditor(new DefaultCellEditor(new JCheckBox()));

        getColumnModel().getColumn(convertColumnIndexToView(RetirementTableModel.COL_MISC_MOD)).
        	setCellEditor(new SpinnerEditor());

        JComboBox<String> cbRecruitType = new JComboBox<String>();
        for (int i = Person.T_NONE; i < Person.T_NUM; i++) {
        	cbRecruitType.addItem(Person.getRoleDesc(i, hqView.getCampaign().getFaction().isClan()));
        }
        getColumnModel().getColumn(convertColumnIndexToView(RetirementTableModel.COL_RECRUIT)).
        	setCellEditor(new DefaultCellEditor(cbRecruitType));
	}

	public void setGeneralMod(int mod) {
		((RetirementTableModel)getModel()).setGeneralMod(mod);
	}

}
