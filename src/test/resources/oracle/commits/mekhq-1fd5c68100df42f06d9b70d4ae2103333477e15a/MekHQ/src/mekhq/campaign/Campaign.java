/*
 * Campaign.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;

import org.joda.time.DateTime;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.client.RandomNameGenerator;
import megamek.client.RandomUnitGenerator;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.BombType;
import megamek.common.Compute;
import megamek.common.ConvFighter;
import megamek.common.Coords;
import megamek.common.Crew;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.FighterSquadron;
import megamek.common.Game;
import megamek.common.GunEmplacement;
import megamek.common.IBomber;
import megamek.common.ITechManager;
import megamek.common.ITechnology;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.LandAirMech;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.SimpleTechLevel;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.logging.LogLevel;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import megamek.common.util.BuildingBlock;
import megamek.common.util.DirectoryItems;
import megamek.common.weapons.bayweapons.BayWeapon;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.NullEntityException;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.event.AcquisitionEvent;
import mekhq.campaign.event.AstechPoolChangedEvent;
import mekhq.campaign.event.DayEndingEvent;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.event.GMModeEvent;
import mekhq.campaign.event.LoanNewEvent;
import mekhq.campaign.event.LoanPaidEvent;
import mekhq.campaign.event.MedicPoolChangedEvent;
import mekhq.campaign.event.MissionNewEvent;
import mekhq.campaign.event.NetworkChangedEvent;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.campaign.event.OvertimeModeEvent;
import mekhq.campaign.event.PartArrivedEvent;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.PartNewEvent;
import mekhq.campaign.event.PartRemovedEvent;
import mekhq.campaign.event.PartWorkEvent;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.event.PersonNewEvent;
import mekhq.campaign.event.PersonRemovedEvent;
import mekhq.campaign.event.PersonTechAssignmentEvent;
import mekhq.campaign.event.ReportEvent;
import mekhq.campaign.event.ScenarioChangedEvent;
import mekhq.campaign.event.ScenarioNewEvent;
import mekhq.campaign.event.UnitNewEvent;
import mekhq.campaign.event.UnitRemovedEvent;
import mekhq.campaign.finances.Asset;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.market.ContractMarket;
import mekhq.campaign.market.PartsStore;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mod.am.InjuryTypes;
import mekhq.campaign.mod.am.InjuryUtil;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.BaArmor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MissingEnginePart;
import mekhq.campaign.parts.MissingMekActuator;
import mekhq.campaign.parts.MissingMekLocation;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.OmniPod;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.parts.ProtomekArmor;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.StructuralIntegrity;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.campaign.parts.equipment.MissingAmmoBin;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.parts.equipment.MissingLargeCraftAmmoBin;
import mekhq.campaign.parts.equipment.MissingMASC;
import mekhq.campaign.personnel.Ancestors;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.Rank;
import mekhq.campaign.personnel.RankTranslator;
import mekhq.campaign.personnel.Ranks;
import mekhq.campaign.personnel.RetirementDefectionTracker;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.rating.CampaignOpsReputation;
import mekhq.campaign.rating.FieldManualMercRevDragoonsRating;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.unit.CrewType;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.unit.UnitTechProgression;
import mekhq.campaign.universe.Era;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.News;
import mekhq.campaign.universe.NewsItem;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planets;
import mekhq.campaign.universe.RATGeneratorConnector;
import mekhq.campaign.universe.RATManager;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.gui.GuiTabType;
import mekhq.gui.dialog.HistoricalDailyReportDialog;
import mekhq.gui.utilities.PortraitFileFactory;
import mekhq.module.atb.AtBEventProcessor;

/**
 * @author Taharqa The main campaign class, keeps track of teams and units
 */
public class Campaign implements Serializable, ITechManager {
    private static final String REPORT_LINEBREAK = "<br/><br/>"; //$NON-NLS-1$

    private static final long serialVersionUID = -6312434701389973056L;

    private UUID id;

    // we have three things to track: (1) teams, (2) units, (3) repair tasks
    // we will use the same basic system (borrowed from MegaMek) for tracking
    // all three
    // OK now we have more, parts, personnel, forces, missions, and scenarios.
    private Map<UUID, Unit> units = new LinkedHashMap<UUID, Unit>();
    private Map<UUID, Person> personnel = new LinkedHashMap<>();
    private Map<UUID, Ancestors> ancestors = new LinkedHashMap<>();
    private Map<Integer, Part> parts = new LinkedHashMap<>();
    private Map<Integer, Force> forceIds = new LinkedHashMap<>();
    private Map<Integer, Mission> missions = new LinkedHashMap<>();
    private Map<Integer, Scenario> scenarios = new LinkedHashMap<>();
    private List<Kill> kills = new ArrayList<>();

    private Map<String, Integer> duplicateNameHash = new HashMap<String, Integer>();

    private int astechPool;
    private int astechPoolMinutes;
    private int astechPoolOvertime;
    private int medicPool;

    private int lastTeamId;
    private int lastPartId;
    private int lastForceId;
    private int lastMissionId;
    private int lastScenarioId;

    // I need to put a basic game object in campaign so that I can
    // assign it to the entities, otherwise some entity methods may get NPE
    // if they try to call up game options
    private Game game;
    private Player player;

    private GameOptions gameOptions;

    private String name;

    private RandomNameGenerator rng;

    // hierarchically structured Force object to define TO&E
    private Force forces;
    private Hashtable<Integer, Lance> lances; //AtB

    // calendar stuff
    public GregorianCalendar calendar;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat shortDateFormat;

    private String factionCode;
    private int techFactionCode;
    private String retainerEmployerCode; //AtB
    private Ranks ranks;

    private ArrayList<String> currentReport;
    private transient String currentReportHTML;
    private transient List<String> newReports;

    //this is updated and used per gaming session, it is enabled/disabled via the Campaign options
    //we're re-using the LogEntry class that is used to store Personnel entries
    public LinkedList<LogEntry> inMemoryLogHistory = new LinkedList<LogEntry>();

    private boolean overtime;
    private boolean gmMode;
    private transient boolean overviewLoadingValue = true;

    private String camoCategory = Player.NO_CAMO;
    private String camoFileName = null;
    private int colorIndex = 0;

    private Finances finances;

    private CurrentLocation location;

    private News news;

    private PartsStore partsStore;

    private ArrayList<String> customs;

    private CampaignOptions campaignOptions = new CampaignOptions();
    private RandomSkillPreferences rskillPrefs = new RandomSkillPreferences();
    private MekHQ app;

    private ShoppingList shoppingList;

    private PersonnelMarket personnelMarket;
    private ContractMarket contractMarket; //AtB
    private UnitMarket unitMarket; //AtB
    private RetirementDefectionTracker retirementDefectionTracker; // AtB
    private int fatigueLevel; //AtB
    private AtBConfiguration atbConfig; //AtB
    private AtBEventProcessor atbEventProcessor; //AtB
    private Calendar shipSearchStart; //AtB
    private int shipSearchType;
    private String shipSearchResult; //AtB
    private Calendar shipSearchExpiration; //AtB
    private IUnitGenerator unitGenerator;
    private IUnitRating unitRating;

    public Campaign() {
        id = UUID.randomUUID();
        game = new Game();
        player = new Player(0, "self");
        game.addPlayer(0, player);
        currentReport = new ArrayList<String>();
        currentReportHTML = "";
        newReports = new ArrayList<String>();
        calendar = new GregorianCalendar(3067, Calendar.JANUARY, 1);
        dateFormat = new SimpleDateFormat("EEEE, MMMM d yyyy");
        shortDateFormat = new SimpleDateFormat("yyyyMMdd");
        name = "My Campaign";
        rng = new RandomNameGenerator();
        rng.populateNames();
        overtime = false;
        gmMode = false;
        factionCode = "MERC";
        techFactionCode = ITechnology.F_MERC;
        retainerEmployerCode = null;
        Ranks.initializeRankSystems();
        ranks = Ranks.getRanksFromSystem(Ranks.RS_SL);
        forces = new Force(name);
        forceIds.put(Integer.valueOf(lastForceId), forces);
        lastForceId++;
        lances = new Hashtable<Integer, Lance>();
        finances = new Finances();
        location = new CurrentLocation(Planets.getInstance().getPlanets()
                .get("Outreach"), 0);
        SkillType.initializeTypes();
        SpecialAbility.initializeSPA();
        astechPool = 0;
        medicPool = 0;
        resetAstechMinutes();
        partsStore = new PartsStore(this);
        gameOptions = new GameOptions();
        gameOptions.initialize();
        gameOptions.getOption("year").setValue(getGameYear());
        game.setOptions(gameOptions);
        customs = new ArrayList<String>();
        shoppingList = new ShoppingList();
        news = new News(getGameYear(), id.getLeastSignificantBits());
        personnelMarket = new PersonnelMarket();
        contractMarket = new ContractMarket();
        unitMarket = new UnitMarket();
        retirementDefectionTracker = new RetirementDefectionTracker();
        fatigueLevel = 0;
        atbConfig = null;
    }

    /**
     * @return the app
     */
    public MekHQ getApp() {
        return app;
    }

    /**
     * @param app the app to set
     */
    public void setApp(MekHQ app) {
        this.app = app;
    }

    /**
     * @return the overviewLoadingValue
     */
    public boolean isOverviewLoadingValue() {
        return overviewLoadingValue;
    }

    /**
     * @param overviewLoadingValue the overviewLoadingValue to set
     */
    public void setOverviewLoadingValue(boolean overviewLoadingValue) {
        this.overviewLoadingValue = overviewLoadingValue;
    }

    public Game getGame() {
        return game;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String s) {
        this.name = s;
    }

    public String getEraName() {
        return Era.getEraNameFromYear(getGameYear());
    }

    public int getEra() {
        return Era.getEra(getGameYear());
    }

    public String getTitle() {
        return getName() + " (" + getFactionName() + ")" + " - "
                + getDateAsString() + " (" + getEraName() + ")";
    }

    public GregorianCalendar getCalendar() {
        return calendar;
    }

    public RandomNameGenerator getRNG() {
        return rng;
    }

    public void setRNG(RandomNameGenerator g) {
        this.rng = g;
    }

    public String getCurrentPlanetName() {
        return location.getCurrentPlanet().getPrintableName(Utilities.getDateTimeDay(calendar));
    }

    public Planet getCurrentPlanet() {
        return location.getCurrentPlanet();
    }

    public long getFunds() {
        return finances.getBalance();
    }

    public Force getForces() {
        return forces;
    }

    public Hashtable<Integer, Lance> getLances() {
        return lances;
    }

    public ArrayList<Lance> getLanceList() {
        ArrayList<Lance> retVal = new ArrayList<Lance>();
        for (Lance l : lances.values()) {
            if (forceIds.containsKey(l.getForceId())) {
                retVal.add(l);
            }
        }
        return retVal;
    }

    public ShoppingList getShoppingList() {
        return shoppingList;
    }

    public PersonnelMarket getPersonnelMarket() {
        return personnelMarket;
    }

    public void generateNewPersonnelMarket() {
        personnelMarket.generatePersonnelForDay(this);
    }

    public ContractMarket getContractMarket() {
        return contractMarket;
    }

    public void generateNewContractMarket() {
        contractMarket.generateContractOffers(this);
    }

    public UnitMarket getUnitMarket() {
        return unitMarket;
    }

    public void generateNewUnitMarket() {
        unitMarket.generateUnitOffers(this);
    }

    public RetirementDefectionTracker getRetirementDefectionTracker() {
        return retirementDefectionTracker;
    }

    public int getFatigueLevel() {
        return fatigueLevel;
    }

    /**
     * Initializes the unit generator based on the method chosen in campaignOptions.
     * Called when the unit generator is first used or when the method has been
     * changed in campaignOptions.
     */
    public void initUnitGenerator() {
        if (unitGenerator != null && unitGenerator instanceof RATManager) {
            MekHQ.unregisterHandler(unitGenerator);
        }
        if (campaignOptions.useStaticRATs()) {
            RATManager rm = new RATManager();
            while (!RandomUnitGenerator.getInstance().isInitialized()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            rm.setSelectedRATs(campaignOptions.getRATs());
            rm.setIgnoreRatEra(campaignOptions.canIgnoreRatEra());
            unitGenerator = rm;
        } else {
            RATGeneratorConnector rgc = new RATGeneratorConnector(getGameYear());
            unitGenerator = rgc;
        }
    }

    /**
     * @return - the class responsible for generating random units
     */
    public IUnitGenerator getUnitGenerator() {
        if (unitGenerator == null) {
            initUnitGenerator();
        }
        return unitGenerator;
    }

    public AtBConfiguration getAtBConfig() {
        if (atbConfig == null) {
            atbConfig = AtBConfiguration.loadFromXml();
        }
        return atbConfig;
    }

    /**
     * 
     * @return The date a ship search was started, or null if none is in progress.
     */
    public Calendar getShipSearchStart() {
        return shipSearchStart;
    }

    /**
     * 
     * @return The lookup name of the available ship, or null if none is available 
     */
    public String getShipSearchResult() {
        return shipSearchResult;
    }

    /**
     * 
     * @return The date the ship is no longer available, if there is one.
     */
    public Calendar getShipSearchExpiration() {
        return shipSearchExpiration;
    }

    public void startShipSearch(int unitType) {
        shipSearchStart = (Calendar) calendar.clone();
        shipSearchType = unitType;
    }

    public void endShipSearch() {
        shipSearchStart = null;
    }

    private void processShipSearch() {
        if (shipSearchStart == null) {
            return;
        }
        StringBuilder report = new StringBuilder();
        if (getFinances().debit(getAtBConfig().shipSearchCostPerWeek(), Transaction.C_UNIT, "Ship search", getDate())) {
            report.append(NumberFormat.getInstance().format(getAtBConfig().shipSearchCostPerWeek())
                    + " C-bills deducted for ship search.");
        } else {
            addReport("<font color=\"red\">Insufficient funds for ship search.</font>");
            shipSearchStart = null;
            return;
        }
        long numDays = TimeUnit.MILLISECONDS.toDays(calendar.getTimeInMillis() - shipSearchStart.getTimeInMillis());
        if (numDays > 21) {
            int roll = Compute.d6(2);
            TargetRoll target = getAtBConfig().shipSearchTargetRoll(shipSearchType, this);
            shipSearchStart = null;
            report.append("<br/>Ship search target: ").append(target.getValueAsString()).append(" roll: ")
                    .append(String.valueOf(roll));
            // TODO: mos zero should make ship available on retainer
            if (roll >= target.getValue()) {
                report.append("<br/>Search successful. ");
                MechSummary ms = unitGenerator.generate(factionCode, shipSearchType, -1,
                        getCalendar().get(Calendar.YEAR), getUnitRatingMod());
                if (ms == null) {
                    ms = getAtBConfig().findShip(shipSearchType);
                }
                if (ms != null) {
                    shipSearchResult = ms.getName();
                    shipSearchExpiration = (Calendar) getCalendar().clone();
                    shipSearchExpiration.add(Calendar.DAY_OF_MONTH, 31);
                    report.append(shipSearchResult).append(" is available for purchase for ")
                            .append(NumberFormat.getInstance().format(ms.getCost())).append(" C-bills until ")
                            .append(dateFormat.format(shipSearchExpiration.getTime()));
                } else {
                    report.append(" <font color=\"red\">Could not determine ship type.</font>");
                }
            } else {
                report.append("<br/>Ship search unsuccessful.");
            }
        }
        addReport(report.toString());
    }

    public void purchaseShipSearchResult() {
        final String METHOD_NAME = "purchaseShipSearchResult()"; //$NON-NLS-1$
        MechSummary ms = MechSummaryCache.getInstance().getMech(shipSearchResult);
        if (ms == null) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                    "Cannot find entry for " + shipSearchResult); //$NON-NLS-1$
            return;
        }

        long cost = ms.getCost();
        if (getFunds() < cost) {
            addReport("<font color='red'><b> You cannot afford this unit. Transaction cancelled</b>.</font>");
            return;
        }

        MechFileParser mechFileParser = null;
        try {
            mechFileParser = new MechFileParser(ms.getSourceFile(),
                    ms.getEntryName());
        } catch (Exception ex) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                    "Unable to load unit: " + ms.getEntryName()); //$NON-NLS-1$
            MekHQ.getLogger().error(getClass(), METHOD_NAME, ex);
        }
        Entity en = mechFileParser.getEntity();

        int transitDays = getCampaignOptions().getInstantUnitMarketDelivery() ? 0
                : calculatePartTransitTime(Compute.d6(2) - 2);

        getFinances().debit(cost, Transaction.C_UNIT, "Purchased " + en.getShortName(), getCalendar().getTime());
        addUnit(en, true, transitDays);
        if (!getCampaignOptions().getInstantUnitMarketDelivery()) {
            addReport("<font color='green'>Unit will be delivered in " + transitDays + " days.</font>");
        }
        shipSearchResult = null;
        shipSearchExpiration = null;
    }

    /**
     * Process retirements for retired personnel, if any.
     * @param totalPayout The total retirement payout.
     * @param unitAssignments List of unit assignments.
     * @return False if there were payments AND they were unable to be processed, true otherwise.
     */
    public boolean applyRetirement(long totalPayout, HashMap<UUID, UUID> unitAssignments) {
        if ((totalPayout > 0) ||
            (null != getRetirementDefectionTracker().getRetirees())) {
            if (getFinances().debit(totalPayout, Transaction.C_SALARY, "Final Payout", getDate())) {
                for (UUID pid : getRetirementDefectionTracker().getRetirees()) {
                    if (getPerson(pid).isActive()) {
                        changeStatus(getPerson(pid), Person.S_RETIRED);
                        addReport(getPerson(pid).getFullName() + " has retired.");
                    }
                    if (Person.T_NONE != getRetirementDefectionTracker().getPayout(pid).getRecruitType()) {
                        getPersonnelMarket().addPerson(
                                newPerson(getRetirementDefectionTracker().getPayout(pid).getRecruitType()));
                    }
                    if (getRetirementDefectionTracker().getPayout(pid).hasHeir()) {
                        Person p = newPerson(getPerson(pid).getPrimaryRole());
                        p.setOriginalUnitWeight(getPerson(pid).getOriginalUnitWeight());
                        p.setOriginalUnitTech(getPerson(pid).getOriginalUnitTech());
                        p.setOriginalUnitId(getPerson(pid).getOriginalUnitId());
                        if (unitAssignments.containsKey(pid)) {
                            getPersonnelMarket().addPerson(p, getUnit(unitAssignments.get(pid)).getEntity());
                        } else {
                            getPersonnelMarket().addPerson(p);
                        }
                    }
                    int dependents = getRetirementDefectionTracker().getPayout(pid).getDependents();
                    while (dependents > 0) {
                        Person p = newPerson(Person.T_ASTECH);
                        p.setDependent(true);
                        if (recruitPerson(p)) {
                            dependents--;
                        } else {
                            dependents = 0;
                        }
                    }
                    if (unitAssignments.containsKey(pid)) {
                        removeUnit(unitAssignments.get(pid));
                    }
                }
                getRetirementDefectionTracker().resolveAllContracts();
                return true;
            } else {
                addReport("<font color='red'>You cannot afford to make the final payments.</font>");
                return false;
            }
        }
        
        return true;
    }

    public News getNews() {
        return news;
    }

    /**
     * Add force to an existing superforce. This method will also assign the force an id and place it in the forceId hash
     *
     * @param force      - the Force to add
     * @param superForce - the superforce to add the new force to
     */
    public void addForce(Force force, Force superForce) {
        int id = lastForceId + 1;
        force.setId(id);
        superForce.addSubForce(force, true);
        force.setScenarioId(superForce.getScenarioId());
        forceIds.put(Integer.valueOf(id), force);
        lastForceId = id;

        if (campaignOptions.getUseAtB() && force.getUnits().size() > 0) {
            if (null == lances.get(Integer.valueOf(id))) {
                lances.put(id, new Lance(force.getId(), this));
            }
        }
    }

    public void moveForce(Force force, Force superForce) {
        Force parentForce = force.getParentForce();
        if (null != parentForce) {
            parentForce.removeSubForce(force.getId());
        }
        superForce.addSubForce(force, true);
        force.setScenarioId(superForce.getScenarioId());
        for (Object o : force.getAllChildren(this)) {
            if (o instanceof Unit) {
                ((Unit) o).setScenarioId(superForce.getScenarioId());
            } else if (o instanceof Force) {
                ((Force) o).setScenarioId(superForce.getScenarioId());
            }
        }
    }

    /**
     * This is used by the XML loader. The id should already be set for this force so dont increment
     *
     * @param force
     */
    public void addForceToHash(Force force) {
        forceIds.put(force.getId(), force);
    }

    /**
     * This is used by the XML loader. The id should already be set for this scenario so dont increment
     *
     * @param scenario
     */
    public void addScenarioToHash(Scenario scenario) {
        scenarios.put(scenario.getId(), scenario);
    }

    /**
     * Add unit to an existing force. This method will also assign that force's id to the unit.
     *
     * @param u
     * @param id
     */
    public void addUnitToForce(Unit u, int id) {
        Force prevForce = forceIds.get(u.getForceId());
        if (null != prevForce) {
            prevForce.removeUnit(u.getId());
            MekHQ.triggerEvent(new OrganizationChangedEvent(prevForce, u));
            if (null != prevForce.getTechID()) {
                u.removeTech();
                Person forceTech = getPerson(prevForce.getTechID());
                forceTech.removeTechUnitId(u.getId());
            }
        }
        Force force = forceIds.get(id);
        if (null != force) {
            u.setForceId(id);
            force.addUnit(u.getId());
            u.setScenarioId(force.getScenarioId());
            MekHQ.triggerEvent(new OrganizationChangedEvent(force, u));
            if (null != force.getTechID()) {
                if (null != u.getTech()) {
                    Person oldTech = u.getTech();
                    oldTech.removeTechUnitId(u.getId());
                    MekHQ.triggerEvent(new PersonTechAssignmentEvent(oldTech, u));
                }
                Person forceTech = getPerson(force.getTechID());
                if (forceTech.canTech(u.getEntity())) {
                    u.setTech(force.getTechID());
                    forceTech.addTechUnitID(u.getId());
                    MekHQ.triggerEvent(new PersonTechAssignmentEvent(forceTech, u));
                } else {
                    String cantTech = forceTech.getName() + " cannot maintain " + u.getName() + "\n"
                            + "You will need to assign a tech manually.";
                    JOptionPane.showMessageDialog(null, cantTech, "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        if (campaignOptions.getUseAtB()) {
            if (null != prevForce && prevForce.getUnits().size() == 0) {
                lances.remove(prevForce.getId());
            }
            if (null == lances.get(id)) {
                lances.put(id, new Lance(force.getId(), this));
            }
        }
    }

    /** Adds force and all its subforces to the AtB lance table
     */

    private void addAllLances(Force force) {
        if (force.getUnits().size() > 0) {
            lances.put(force.getId(), new Lance(force.getId(), this));
        }
        for (Force f : force.getSubForces()) {
            addAllLances(f);
        }
    }



    /**
     * Add a mission to the campaign
     *
     * @param m The mission to be added
     */
    public int addMission(Mission m) {
        int id = lastMissionId + 1;
        m.setId(id);
        missions.put(Integer.valueOf(id), m);
        lastMissionId = id;
        MekHQ.triggerEvent(new MissionNewEvent(m));
        return id;
    }

    private void addMissionWithoutId(Mission m) {
        missions.put(Integer.valueOf(m.getId()), m);

        if (m.getId() > lastMissionId) {
            lastMissionId = m.getId();
        }
        MekHQ.triggerEvent(new MissionNewEvent(m));
    }

    /**
     * @return an <code>Collection</code> of missions in the campaign
     */
    public Collection<Mission> getMissions() {
        return missions.values();
    }

    /**
     * Add scenario to an existing mission. This method will also assign the scenario an id and place it in the scenarioId
     * hash
     *
     * @param s - the Scenario to add
     * @param m - the mission to add the new scenario to
     */
    public void addScenario(Scenario s, Mission m) {
        int id = lastScenarioId + 1;
        s.setId(id);
        m.addScenario(s);
        scenarios.put(Integer.valueOf(id), s);
        lastScenarioId = id;
        MekHQ.triggerEvent(new ScenarioNewEvent(s));
    }

    /**
     * @return missions arraylist sorted with complete missions at the bottom
     */
    public ArrayList<Mission> getSortedMissions() {
        ArrayList<Mission> msns = new ArrayList<>(missions.values());
        Collections.sort(msns, new Comparator<Mission>() {
            @Override
            public int compare(final Mission m1, final Mission m2) {
                return Boolean.compare(m2.isActive(), m1.isActive());
            }
        });

        return msns;
    }

    /**
     * @param id the <code>int</code> id of the team
     * @return a <code>SupportTeam</code> object
     */
    public Mission getMission(int id) {
        return missions.get(Integer.valueOf(id));
    }

    public Scenario getScenario(int id) {
        return scenarios.get(Integer.valueOf(id));
    }

    public CurrentLocation getLocation() {
        return location;
    }

    private void addUnit(Unit u) {
        MekHQ.getLogger().log(getClass(), "addUnit()", LogLevel.INFO, //$NON-NLS-1$
                "Adding unit: (" + u.getId() + "):" + u); //$NON-NLS-1$
        units.put(u.getId(), u);
        checkDuplicateNamesDuringAdd(u.getEntity());

        // Assign an entity ID to our new unit
        if (Entity.NONE == u.getEntity().getId()) {
            u.getEntity().setId(game.getNextEntityId());
        }
        game.addEntity(u.getEntity().getId(), u.getEntity());
    }

    /**
     * This is for adding a TestUnit that was previously created and had parts added to
     * it. We need to do the normal stuff, but we also need to take the existing parts and
     * add them to the campaign.
     * @param unit
     */
    public void addTestUnit(TestUnit tu) {
        // we really just want the entity and the parts so lets just wrap that around a
        // new
        // unit.
        Unit unit = new Unit(tu.getEntity(), this);

        // we decided we like the test unit so much we are going to keep it
        unit.getEntity().setOwner(player);
        unit.getEntity().setGame(game);

        UUID id = UUID.randomUUID();
        // check for the very rare chance of getting same id
        while (null != units.get(id)) {
            id = UUID.randomUUID();
        }
        unit.getEntity().setExternalIdAsString(id.toString());
        unit.setId(id);
        units.put(id, unit);

        // now lets grab the parts from the test unit and set them up with this unit
        for(Part p : tu.getParts()) {
            unit.addPart(p);
            addPart(p, 0);
        }

        unit.resetPilotAndEntity();

        if (!unit.isRepairable()) {
            unit.setSalvage(true);
        }

        // Assign an entity ID to our new unit
        if (Entity.NONE == unit.getEntity().getId()) {
            unit.getEntity().setId(game.getNextEntityId());
        }
        game.addEntity(unit.getEntity().getId(), unit.getEntity());

        checkDuplicateNamesDuringAdd(unit.getEntity());
        addReport(unit.getHyperlinkedName() + " has been added to the unit roster.");

    }

    /**
     * Add a unit to the campaign. This is only for new units
     *
     * @param en An <code>Entity</code> object that the new unit will be wrapped around
     */
    public void addUnit(Entity en, boolean allowNewPilots, int days) {
        // reset the game object
        en.setOwner(player);
        en.setGame(game);

        UUID id = UUID.randomUUID();
        // check for the very rare chance of getting same id
        while (null != units.get(id)) {
            id = UUID.randomUUID();
        }
        en.setExternalIdAsString(id.toString());
        Unit unit = new Unit(en, this);
        unit.setId(id);
        units.put(id, unit);
        removeUnitFromForce(unit); // Added to avoid the 'default force bug'
        // when calculating cargo

        unit.initializeParts(true);
        unit.runDiagnostic(false);
        if (!unit.isRepairable()) {
            unit.setSalvage(true);
        }
        unit.setDaysToArrival(days);

        if (allowNewPilots) {
            Map<CrewType, Collection<Person>> newCrew = Utilities.genRandomCrewWithCombinedSkill(this, unit, getFactionCode());
            newCrew.forEach((type, personnel) -> personnel.forEach(p -> type.addMethod.accept(unit, p)));
        }
        unit.resetPilotAndEntity();

        // Assign an entity ID to our new unit
        if (Entity.NONE == en.getId()) {
            en.setId(game.getNextEntityId());
        }
        game.addEntity(en.getId(), en);

        checkDuplicateNamesDuringAdd(en);
        addReport(unit.getHyperlinkedName() + " has been added to the unit roster.");
        MekHQ.triggerEvent(new UnitNewEvent(unit));
    }

    public Collection<Unit> getUnits() {
        return units.values();
    }

    public ArrayList<Unit> getUnits(boolean weightSorted, boolean alphaSorted) {
        ArrayList<Unit> sortedUnits = getCopyOfUnits();
        if (alphaSorted || weightSorted) {
            if (alphaSorted) {
                Collections.sort(sortedUnits, new Comparator<Unit>() {
                    @Override
                    public int compare(Unit lhs, Unit rhs) {
                        // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                        return lhs.getName().compareTo(rhs.getName());
                    }
                });
            }
            if (weightSorted) {
                Collections.sort(sortedUnits, new Comparator<Unit>() {
                    @Override
                    public int compare(Unit lhs, Unit rhs) {
                        // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                        return lhs.getEntity().getWeight() > rhs.getEntity().getWeight() ? -1 : (lhs.getEntity().getWeight() < rhs.getEntity().getWeight()) ? 1 : 0;
                    }
                });
            }
        }
        return sortedUnits;
    }

    // Since getUnits doesn't return a defensive copy and I don't know what I might break if I made it do so...
    public ArrayList<Unit> getCopyOfUnits() {
        return new ArrayList<>(units.values());
    }

    public ArrayList<Entity> getEntities() {
        ArrayList<Entity> entities = new ArrayList<Entity>();
        for (Unit unit : getUnits()) {
            entities.add(unit.getEntity());
        }
        return entities;
    }

    public Unit getUnit(UUID id) {
        if (null == id) {
            return null;
        }
        return units.get(id);
    }

    public boolean recruitPerson(Person p) {
        return recruitPerson(p, false, true);
    }

    public boolean recruitPerson(Person p, boolean log) {
        return recruitPerson(p, false, log);
    }

    public boolean recruitPerson(Person p, boolean prisoner, boolean log) {
        if (p == null) {
            return false;
        }
        // Only pay if option set and this isn't a prisoner or bondsman
        if (getCampaignOptions().payForRecruitment() && !prisoner) {
            if (!getFinances().debit(2 * p.getSalary(), Transaction.C_SALARY,
                    "recruitment of " + p.getFullName(), getCalendar().getTime())) {
                addReport("<font color='red'><b>Insufficient funds to recruit "
                        + p.getFullName() + "</b></font>");
                return false;
            }
        }
        UUID id = UUID.randomUUID();
        while (null != personnel.get(id)) {
            id = UUID.randomUUID();
        }
        p.setId(id);
        personnel.put(id, p);

        //TODO: implement a boolean check based on campaign options
        boolean bondsman = false;
        String add = prisoner == true ? " as a prisoner" : bondsman == true ? " as a bondsman" : "";
        if (log) {
            addReport(p.getHyperlinkedName() + " has been added to the personnel roster"+add+".");
        }
        if (p.getPrimaryRole() == Person.T_ASTECH) {
            astechPoolMinutes += 480;
            astechPoolOvertime += 240;
        }
        if (p.getSecondaryRole() == Person.T_ASTECH) {
            astechPoolMinutes += 240;
            astechPoolOvertime += 120;
        }
        String rankEntry = "";
        if (p.getRankNumeric() > 0) {
            rankEntry = " as a " + p.getRankName();
        }
        if (prisoner) {
            if (getCampaignOptions().getDefaultPrisonerStatus() == CampaignOptions.BONDSMAN_RANK) {
                p.setBondsman();
                if (log) {
                    p.addLogEntry(getDate(), "Made Bondsman " + getName() + rankEntry);
                }
            } else {
                p.setPrisoner();
                if (log) {
                    p.addLogEntry(getDate(), "Made Prisoner " + getName() + rankEntry);
                }
            }
        } else {
            p.setFreeMan();
            if (log) {
                p.addLogEntry(getDate(), "Joined " + getName() + rankEntry);
            }
        }
        MekHQ.triggerEvent(new PersonNewEvent(p));
        return true;
    }

    private void addPersonWithoutId(Person p) {
        personnel.put(p.getId(), p);
        MekHQ.triggerEvent(new PersonNewEvent(p));
    }

    private void addAncestorsWithoutId(Ancestors a) {
        ancestors.put(a.getId(), a);
    }

    public void addPersonWithoutId(Person p, boolean log) {
        while((null == p.getId()) || (null != personnel.get(p.getId()))) {
            p.setId(UUID.randomUUID());
        }
        addPersonWithoutId(p);
        if (log) {
            addReport(p.getHyperlinkedName() + " has been added to the personnel roster.");
        }
        if (p.getPrimaryRole() == Person.T_ASTECH) {
            astechPoolMinutes += 480;
            astechPoolOvertime += 240;
        }
        if (p.getSecondaryRole() == Person.T_ASTECH) {
            astechPoolMinutes += 240;
            astechPoolOvertime += 120;
        }
        String rankEntry = "";
        if (p.getRankNumeric() > 0) {
            rankEntry = " as a " + p.getRankName();
        }
        p.addLogEntry(getDate(), "Joined " + getName() + rankEntry);
    }

    public Date getDate() {
        return calendar.getTime();
    }

    public Collection<Person> getPersonnel() {
        return personnel.values();
    }

    /**
     * Provides a filtered list of personnel including only active Persons.
     * @return ArrayList<Person>
     */
    public ArrayList<Person> getActivePersonnel() {
        ArrayList<Person> activePersonnel = new ArrayList<Person>();
        for (Person p : getPersonnel()) {
            if (p.isActive()) {activePersonnel.add(p);}
        }
        return activePersonnel;
    }

    public Iterable<Ancestors> getAncestors() {
        return ancestors.values();
    }

    /** @return a matching ancestors entry for the arguments, or null if there isn't any */
    public Ancestors getAncestors(UUID fatherID, UUID motherID) {
        for(Map.Entry<UUID, Ancestors> m : ancestors.entrySet()) {
            Ancestors a = m.getValue();
            if(Objects.equals(fatherID, a.getFatherID()) && Objects.equals(motherID, a.getMotherID())) {
                return a;
            }
        }
        return null;
    }

    public ArrayList<Person> getPatients() {
        ArrayList<Person> patients = new ArrayList<Person>();
        for (Person p : getPersonnel()) {
            if (p.needsFixing()
                    || (getCampaignOptions().useAdvancedMedical() && p.hasInjuries(true) && p.isActive())) {
                patients.add(p);
            }
        }
        return patients;
    }

    public ArrayList<Unit> getServiceableUnits() {
        ArrayList<Unit> service = new ArrayList<Unit>();
        for (Unit u : getUnits()) {
            if (!u.isAvailable()) {
                continue;
            }
            if (u.isServiceable()) {
                service.add(u);
            }
        }
        return service;
    }

    public Person getPerson(UUID id) {
        if (id == null) {
            return null;
        }
        return personnel.get(id);
    }

    public Ancestors getAncestors(UUID id) {
        if (id == null) {
            return null;
        }
        return ancestors.get(id);
    }

    public Ancestors createAncestors(UUID father, UUID mother) {
        Ancestors na = new Ancestors(father, mother, this);
        ancestors.put(na.getId(), na);
        return na;
    }

    public void addPart(Part p, int transitDays) {
        if (null != p.getUnit() && p.getUnit() instanceof TestUnit) {
            // if this is a test unit, then we won't add the part, so there
            return;
        }
        p.setDaysToArrival(transitDays);
        p.setBrandNew(false);
        //need to add ID here in case post-processing part stuff needs it
        //we will set the id back one if we don't end up adding this part
        int id = lastPartId + 1;
        p.setId(id);
        lastPartId = id;
        //be careful in using this next line
        p.postProcessCampaignAddition();
        // dont add missing parts if they dont have units or units with not id
        if (p instanceof MissingPart
                && (null == p.getUnit() || null == p.getUnitId())) {
            lastPartId = lastPartId - 1;
            return;
        }
        Part spare = checkForExistingSparePart(p);
        if (null == p.getUnit() && null != spare) {
            if (p instanceof Armor) {
                if (spare instanceof Armor) {
                    ((Armor) spare).setAmount(((Armor) spare).getAmount()
                            + ((Armor) p).getAmount());
                    lastPartId = lastPartId - 1;
                    MekHQ.triggerEvent(new PartChangedEvent(spare));
                    return;
                }
            } else if (p instanceof AmmoStorage) {
                if (spare instanceof AmmoStorage) {
                    ((AmmoStorage) spare).changeShots(((AmmoStorage) p)
                            .getShots());
                    lastPartId = lastPartId - 1;
                    MekHQ.triggerEvent(new PartChangedEvent(spare));
                    return;
                }
            } else {
                spare.incrementQuantity();
                lastPartId = lastPartId - 1;
                MekHQ.triggerEvent(new PartChangedEvent(spare));
                return;
            }
        }
        parts.put(Integer.valueOf(id), p);
        MekHQ.triggerEvent(new PartNewEvent(p));
    }

    /**
     * This is similar to addPart, but we just check to see if this part can be added to an existing part, without actually
     * adding it to the campaign (because its already there). Should be called up when a part goes from 1 daysToArrival to
     * zero.
     *
     * @param p
     */
    public void arrivePart(Part p) {
        if (null != p.getUnit()) {
            return;
        }
        p.setDaysToArrival(0);
        addReport(p.getArrivalReport());
        int quantity = p.getQuantity();
        Part spare = checkForExistingSparePart(p);
        if (null != spare) {
            if (p instanceof Armor) {
                if (spare instanceof Armor) {
                    while (quantity > 0) {
                        ((Armor) spare).setAmount(((Armor) spare).getAmount()
                                + ((Armor) p).getAmount());
                        quantity--;
                    }
                    removePart(p);
                }
            } else if (p instanceof AmmoStorage) {
                if (spare instanceof AmmoStorage) {
                    while (quantity > 0) {
                        ((AmmoStorage) spare).changeShots(((AmmoStorage) p)
                                .getShots());
                        quantity--;
                    }
                    removePart(p);
                }
            } else {
                while (quantity > 0) {
                    spare.incrementQuantity();
                    quantity--;
                }
                removePart(p);
            }
            MekHQ.triggerEvent(new PartArrivedEvent(spare));
        } else {
            MekHQ.triggerEvent(new PartArrivedEvent(p));            
        }
    }

    public void addPartWithoutId(Part p) {
        if (p instanceof MissingPart && null == p.getUnitId()) {
            // we shouldn't have spare missing parts. I think their existence is
            // a relic.
            return;
        }
        // go ahead and check for existing parts because some version weren't
        // properly collecting parts
        if (!(p instanceof MissingPart)) {
            Part spare = checkForExistingSparePart(p);
            if (null == p.getUnitId() && null != spare) {
                if (p instanceof Armor) {
                    if (spare instanceof Armor) {
                        ((Armor) spare).setAmount(((Armor) spare).getAmount()
                                + ((Armor) p).getAmount());
                        return;
                    }
                } else if (p instanceof AmmoStorage) {
                    if (spare instanceof AmmoStorage) {
                        ((AmmoStorage) spare).changeShots(((AmmoStorage) p)
                                .getShots());
                        return;
                    }
                } else {
                    spare.incrementQuantity();
                    return;
                }
            }
        }
        parts.put(p.getId(), p);

        if (p.getId() > lastPartId) {
            lastPartId = p.getId();
        }
        MekHQ.triggerEvent(new PartNewEvent(p));
    }

    /**
     * @return an <code>ArrayList</code> of SupportTeams in the campaign
     */
    public Collection<Part> getParts() {
        return parts.values();
    }

    private int getQuantity(Part p) {
        if(p instanceof Armor) {
            return ((Armor) p).getAmount();
        }
        if(p instanceof AmmoStorage) {
            return ((AmmoStorage) p).getShots();
        }
        return ((p.getUnit() != null) || (p.getUnitId() != null)) ? 1 : p.getQuantity();
    }

    private PartInUse getPartInUse(Part p) {
        // SI isn't a proper "part"
        if (p instanceof StructuralIntegrity) {
            return null;
        }
        // Makes no sense buying those separately from the chasis
        if((p instanceof EquipmentPart)
                && ((EquipmentPart) p).getType() != null
                && (((EquipmentPart) p).getType().hasFlag(MiscType.F_CHASSIS_MODIFICATION)))
        {
            return null;
        }
        // Replace a "missing" part with a corresponding "new" one.
        if(p instanceof MissingPart) {
            p = ((MissingPart) p).getNewPart();
        }
        PartInUse result = new PartInUse(p);
        return (null != result.getPartToBuy()) ? result : null;
    }

    private void updatePartInUseData(PartInUse piu, Part p) {
        if ((p.getUnit() != null) || (p.getUnitId() != null) || (p instanceof MissingPart)) {
            piu.setUseCount(piu.getUseCount() + getQuantity(p));
        } else {
            if(p.isPresent()) {
                piu.setStoreCount(piu.getStoreCount() + getQuantity(p));
            } else {
                piu.setTransferCount(piu.getTransferCount() + getQuantity(p));
            }
        }
    }

    /** Update the piu with the current campaign data */
    public void updatePartInUse(PartInUse piu) {
        piu.setUseCount(0);
        piu.setStoreCount(0);
        piu.setTransferCount(0);
        piu.setPlannedCount(0);
        for(Part p : getParts()) {
            PartInUse newPiu = getPartInUse(p);
            if(piu.equals(newPiu)) {
                updatePartInUseData(piu, p);
            }
        }
        for(IAcquisitionWork maybePart : shoppingList.getPartList()) {
            PartInUse newPiu = getPartInUse((Part) maybePart);
            if(piu.equals(newPiu)) {
                piu.setPlannedCount(piu.getPlannedCount()
                        + getQuantity((maybePart instanceof MissingPart) ? ((MissingPart) maybePart).getNewPart()
                                : (Part) maybePart) * maybePart.getQuantity());
            }
        }
    }

    public Set<PartInUse> getPartsInUse() {
        // java.util.Set doesn't supply a get(Object) method, so we have to use a java.util.Map
        Map<PartInUse, PartInUse> inUse = new HashMap<PartInUse, PartInUse>();
        for(Part p : getParts()) {
            PartInUse piu = getPartInUse(p);
            if(null == piu) {
                continue;
            }
            if( inUse.containsKey(piu) ) {
                piu = inUse.get(piu);
            } else {
                inUse.put(piu, piu);
            }
            updatePartInUseData(piu, p);
        }
        for(IAcquisitionWork maybePart : shoppingList.getPartList()) {
            if(!(maybePart instanceof Part)) {
                continue;
            }
            PartInUse piu = getPartInUse((Part) maybePart);
            if(null == piu) {
                continue;
            }
            if( inUse.containsKey(piu) ) {
                piu = inUse.get(piu);
            } else {
                inUse.put(piu, piu);
            }
            piu.setPlannedCount(piu.getPlannedCount()
                    + getQuantity((maybePart instanceof MissingPart) ? ((MissingPart) maybePart).getNewPart()
                            : (Part) maybePart) * maybePart.getQuantity());

        }
        return inUse.keySet();
    }

    public Part getPart(int id) {
        return parts.get(Integer.valueOf(id));
    }

    public Force getForce(int id) {
        return forceIds.get(Integer.valueOf(id));
    }

    public ArrayList<String> getCurrentReport() {
        return currentReport;
    }

    public String getCurrentReportHTML() {
        return currentReportHTML;
    }

    public List<String> fetchAndClearNewReports() {
        List<String> oldReports = newReports;
        newReports = new ArrayList<String>();
        return oldReports;
    }

    /**
     * Finds the active person in a particular role with the highest level in a
     * given, with an optional secondary skill to break ties.
     *
     * @param role
     *            One of the Person.T_* constants
     * @param primary
     *            The skill to use for comparison.
     * @param secondary
     *            If not null and there is more than one person tied for the most
     *            the highest, preference will be given to the one with a higher
     *            level in the secondary skill.
     * @return The admin in the designated role with the most experience.
     */
    public Person findBestInRole(int role, String primary, String secondary) {
        int highest = 0;
        Person retVal = null;
        for (Person p : getPersonnel()) {
            if (p.isActive() && (p.getPrimaryRole() == role || p.getSecondaryRole() == role)
                    && p.getSkill(primary) != null) {
                if (p.getSkill(primary).getLevel() > highest) {
                    retVal = p;
                    highest = p.getSkill(primary).getLevel();
                } else if (secondary != null && p.getSkill(primary).getLevel() == highest &&
                /*
                 * If the skill level of the current person is the same as the previous highest,
                 * select the current instead under the following conditions:
                 */
                        (retVal == null || // None has been selected yet (current has level 0)
                                retVal.getSkill(secondary) == null || // Previous selection does not have secondary
                                                                      // skill
                                (p.getSkill(secondary) != null // Current has secondary skill and it is higher than the
                                                               // previous.
                                        && p.getSkill(secondary).getLevel() > retVal.getSkill(secondary).getLevel()))) {
                    retVal = p;
                }
            }
        }
        return retVal;
    }

    public Person findBestInRole(int role, String skill) {
        return findBestInRole(role, skill, null);
    }

    /**
     * Returns a list of active technicians.
     *
     * @param noZeroMinute
     *            If TRUE, then techs with no time remaining will be excluded from
     *            the list.
     * @param firstTechId
     *            The ID of the tech that should appear first in the list (assuming
     *            active and satisfies the noZeroMinute argument)
     * @param sorted
     *            If TRUE, then return the list sorted from worst to best
     * @param eliteFirst
     *            If TRUE and sorted also TRUE, then return the list sorted from
     *            best to worst
     * @return The list of active {@link Person}s who qualify as technicians
     *         ({@link Person#isTech()}).
     */
    public ArrayList<Person> getTechs(boolean noZeroMinute, UUID firstTechId, boolean sorted, boolean eliteFirst) {
        ArrayList<Person> techs = new ArrayList<>();

        // Get the first tech.
        Person firstTech = getPerson(firstTechId);
        if ((firstTech != null) && firstTech.isTech() && firstTech.isActive()
                && (!noZeroMinute || firstTech.getMinutesLeft() > 0)) {
            techs.add(firstTech);
        }

        for (Person p : getPersonnel()) {
            if (p.isTech() && p.isActive() && (!p.equals(firstTech)) && (!noZeroMinute || (p.getMinutesLeft() > 0))) {
                techs.add(p);
            }
        }
        // also need to loop through and collect engineers on self-crewed vessels
        for (Unit u : getUnits()) {
            if (u.isSelfCrewed() && !(u.getEntity() instanceof Infantry) && null != u.getEngineer()) {
                techs.add(u.getEngineer());
            }
        }

        // Return the tech collection sorted worst to best
        // Reverse the sort if we've been asked for best to worst
        if (sorted) {
            Collections.sort(techs, new Comparator<Person>() {
                @Override
                public int compare(Person person1, Person person2) {
                    // default to 0, which means they're equal
                    int retVal = 0;

                    // Set up booleans to know if the tech is secondary only
                    // this is to get the skill from getExperienceLevel(boolean) properly
                    boolean p1Secondary = !person1.isTechPrimary() && person1.isTechSecondary();
                    boolean p2Secondary = !person2.isTechPrimary() && person2.isTechSecondary();

                    if (person1.getExperienceLevel(p1Secondary)
                            > person2.getExperienceLevel(p2Secondary)) {
                        // Person 1 is better than Person 2.
                        retVal = -1;
                    } else if (person1.getExperienceLevel(p1Secondary)
                            < person2.getExperienceLevel(p2Secondary)) {
                        // Person 2 is better than Person 1
                        retVal = 1;
                    }

                    // Return, swapping the value if we're looking to have Elites ordered first
                    return eliteFirst ? retVal *= -1 : retVal;
                }

            });
        }

        return techs;
    }

    public ArrayList<Person> getTechs(boolean noZeroMinute) {
        return getTechs(noZeroMinute, null, true, false);
    }

    /**
     * @return The list of all active {@link Person}s who qualify as technicians ({@link Person#isTech()}));
     */
    public ArrayList<Person> getTechs() {
        return getTechs(false, null, true, false);
    }
    
    /**
     * Gets a list of all techs of a specific role type
     * @param roleType The filter role type
     * @return Collection of all techs that match the given tech role
     */
    public List<Person> getTechsByRole(int roleType) {
        List<Person> techs = getTechs(false, null, false, false);
        List<Person> retval = new ArrayList<>();
        
        for(Person tech : techs) {
            if((tech.getPrimaryRole() == roleType) ||
                    (tech.getSecondaryRole() == roleType)) {
                retval.add(tech);
            }       
        }
        
        return techs;
    }

    public List<Person> getAdmins() {
        List<Person> admins = new ArrayList<Person>();
        for (Person p : getPersonnel()) {
            if (p.isAdmin() && p.isActive()) {
                admins.add(p);
            }
        }
        return admins;
    }

    public boolean isWorkingOnRefit(Person p) {
        for (Map.Entry<UUID, Unit> mu : units.entrySet()) {
            Unit u = mu.getValue();
            if (u.isRefitting()) {
                if (null != u.getRefit().getTeamId()
                        && u.getRefit().getTeamId().equals(p.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<Person> getDoctors() {
        ArrayList<Person> docs = new ArrayList<Person>();
        for (Person p : getPersonnel()) {
            if (p.isDoctor() && p.isActive()) {
                docs.add(p);
            }
        }
        return docs;
    }

    public int getPatientsFor(Person doctor) {
        int patients = 0;
        for (Person person : getPersonnel()) {
            if (null != person.getDoctorId()
                    && person.getDoctorId().equals(doctor.getId())
                    && person.isActive()) {
                patients++;
            }
        }
        return patients;
    }

    /**
     * return an html report on this unit. This will go in MekInfo
     *
     * @param
     * @return
     */
    /*
     * public String getUnitDesc(UUID unitId) { Unit unit = getUnit(unitId); String
     * toReturn = "<html><font size='2'"; if (unit.isDeployed()) { toReturn +=
     * " color='white'"; } toReturn += ">"; toReturn += unit.getDescHTML(); int
     * totalMin = 0; int total = 0; // int cost = unit.getRepairCost();
     * 
     * if (total > 0) { toReturn += "Total tasks: " + total + " (" + totalMin +
     * " minutes)<br/>"; } /* if (cost > 0) { NumberFormat numberFormat =
     * DecimalFormat.getIntegerInstance(); String text = numberFormat.format(cost) +
     * " " + (cost != 0 ? "CBills" : "CBill"); toReturn += "Repair cost : " + text +
     * "<br/>"; }
     */
    /*
     * toReturn += "</font>"; toReturn += "</html>"; return toReturn; }
     */
    public String healPerson(Person medWork, Person doctor) {
        if (getCampaignOptions().useAdvancedMedical()) {
            return "";
        }
        String report = "";
        report += doctor.getHyperlinkedFullTitle() + " attempts to heal "
                + medWork.getFullName();
        TargetRoll target = getTargetFor(medWork, doctor);
        int roll = Compute.d6(2);
        report = report + ",  needs " + target.getValueAsString()
                + " and rolls " + roll + ":";
        int xpGained = 0;
        //If we get a natural 2 that isn't an automatic success, reroll if Edge is available and in use.
        if (getCampaignOptions().useSupportEdge()
                && (doctor.getOptions().booleanOption(PersonnelOptions.EDGE_MEDICAL))) {
            if (roll == 2  && doctor.getEdge() > 0 && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                doctor.setEdge(doctor.getEdge() - 1);
                roll = Compute.d6(2);
                report += medWork.fail(0) + "\n" + doctor.getHyperlinkedFullTitle() + " uses Edge to reroll:"
                        + " rolls " + roll + ":";
            }
        }
        if (roll >= target.getValue()) {
            report = report + medWork.succeed();
            Unit u = getUnit(medWork.getUnitId());
            if (null != u) {
                u.resetPilotAndEntity();
            }
            if (roll == 12 && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                xpGained += getCampaignOptions().getSuccessXP();
            }
            if (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                doctor.setNTasks(doctor.getNTasks() + 1);
            }
            if (doctor.getNTasks() >= getCampaignOptions().getNTasksXP()) {
                xpGained += getCampaignOptions().getTaskXP();
                doctor.setNTasks(0);
            }
        } else {
            report = report + medWork.fail(0);
            if (roll == 2 && target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
                xpGained += getCampaignOptions().getMistakeXP();
            }
        }
        if (xpGained > 0) {
            doctor.setXp(doctor.getXp() + xpGained);
            report += " (" + xpGained + "XP gained) ";
        }
        medWork.setDaysToWaitForHealing(getCampaignOptions()
                .getHealingWaitingPeriod());
        return report;
    }

    public TargetRoll getTargetFor(Person medWork, Person doctor) {
        Skill skill = doctor.getSkill(SkillType.S_DOCTOR);
        if (null == skill) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, doctor.getFullName()
                    + " isn't a doctor, he just plays one on TV.");
        }
        if (medWork.getDoctorId() != null
                && !medWork.getDoctorId().equals(doctor.getId())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    medWork.getFullName() + " is already being tended by another doctor");
        }
        if (!medWork.needsFixing()
                && !(getCampaignOptions().useAdvancedMedical() && medWork.needsAMFixing())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    medWork.getFullName() + " does not require healing.");
        }
        if (getPatientsFor(doctor) > 25) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, doctor.getFullName()
                    + " already has 25 patients.");
        }
        TargetRoll target = new TargetRoll(skill.getFinalSkillValue(),
                SkillType.getExperienceLevelName(skill.getExperienceLevel()));
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            return target;
        }
        // understaffed mods
        int helpMod = getShorthandedMod(getMedicsPerDoctor(), true);
        if (helpMod > 0) {
            target.addModifier(helpMod, "shorthanded");
        }
        target.append(medWork.getHealingMods(doctor));
        return target;
    }

    public Person getLogisticsPerson() {
        int bestSkill = -1;
        int maxAcquisitions = getCampaignOptions().getMaxAcquisitions();
        Person admin = null;
        String skill = getCampaignOptions().getAcquisitionSkill();
        if (skill.equals(CampaignOptions.S_AUTO)) {
            return admin;
        } else if (skill.equals(CampaignOptions.S_TECH)) {
            for (Person p : getPersonnel()) {
                if (getCampaignOptions().isAcquisitionSupportStaffOnly()
                        && !p.isSupport()) {
                    continue;
                }
                if (maxAcquisitions > 0 && p.getAcquisitions() >= maxAcquisitions) {
                    continue;
                }
                if (p.isActive() && null != p.getBestTechSkill()
                        && p.getBestTechSkill().getLevel() > bestSkill) {
                    admin = p;
                    bestSkill = p.getBestTechSkill().getLevel();
                }
            }
        } else {
            for (Person p : getPersonnel()) {
                if (getCampaignOptions().isAcquisitionSupportStaffOnly()
                        && !p.isSupport()) {
                    continue;
                }
                if (maxAcquisitions > 0 && p.getAcquisitions() >= maxAcquisitions) {
                    continue;
                }
                if (p.isActive() && p.hasSkill(skill)
                        && p.getSkill(skill).getLevel() > bestSkill) {
                    admin = p;
                    bestSkill = p.getSkill(skill).getLevel();
                }
            }
        }
        return admin;
    }

    public ShoppingList goShopping(ShoppingList sList) {
    	
    	//get the logistics person and return original list with a message if you don't have one
    	Person person = getLogisticsPerson();
        if(null == person && !getCampaignOptions().getAcquisitionSkill().equals(CampaignOptions.S_AUTO)) {
            addReport("Your force has no one capable of acquiring equipment.");
            return sList;
        }
        
        //loop through shopping list. If its time to check, then check as appropriate. Items not
        //found get added to the remaining item list
        ArrayList<IAcquisitionWork> remainingItems = new ArrayList<IAcquisitionWork>();
        for(IAcquisitionWork shoppingItem : sList.getAllShoppingItems()) {
            shoppingItem.decrementDaysToWait();
            
            if(shoppingItem.getDaysToWait() <= 0) {
                while(shoppingItem.getQuantity() > 0) {
                	if(!acquireEquipment(shoppingItem)) {
                		break;
                	}
                }
            }
            if(shoppingItem.getQuantity() > 0 || shoppingItem.getDaysToWait() > 0) {
            	remainingItems.add(shoppingItem);
            }
        }
        
        return new ShoppingList(remainingItems);
        
    }
    
    public boolean acquireEquipment(IAcquisitionWork acquisition) {
        boolean found = false;
        String report = "";

        Person person = getLogisticsPerson();
        if(null == person && !getCampaignOptions().getAcquisitionSkill().equals(CampaignOptions.S_AUTO)) {
            addReport("Your force has no one capable of acquiring equipment.");
            return false;
        }
        
        //check on funds
        if(((acquisition instanceof UnitOrder && getCampaignOptions().payForUnits()) 
        		||(acquisition instanceof Part && getCampaignOptions().payForParts())) 
        		&& getFunds() < acquisition.getBuyCost()) {
            addReport("<font color='red'><b>You cannot afford to purchase " + acquisition.getAcquisitionName() + "</b></font>");
            return false;
       }
        
        TargetRoll target = getTargetForAcquisition(acquisition, person, false);
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            addReport(target.getDesc());
            return false;
        }
        if (null != person) {
            report += person.getHyperlinkedFullTitle() + " ";
        }
        report += "attempts to find " + acquisition.getAcquisitionName();
        int roll = Compute.d6(2);
        report += "  needs " + target.getValueAsString();
        report += " and rolls " + roll + ":";
        //Edge reroll, if applicable
        if (roll < target.getValue()
                && getCampaignOptions().useSupportEdge() 
                && person.getOptions().booleanOption(PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL)
                && person.getEdge() > 0) {
            person.setEdge(person.getEdge() - 1);
            roll = Compute.d6(2);
            report += " <b>failed!</b> but uses Edge to reroll...getting a " + roll + ": ";
        }
        int mos = roll - target.getValue();
        if (target.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            mos = roll - 2;
        }
        int xpGained = 0;
        if (roll >= target.getValue()) {
            int transitDays = calculatePartTransitTime(mos);
            report = report + acquisition.find(transitDays);
            found = true;
            if (person != null) {
                if (roll == 12
                        && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                    xpGained += getCampaignOptions().getSuccessXP();
                }
                if (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                    person.setNTasks(person.getNTasks() + 1);
                }
                if (person.getNTasks() >= getCampaignOptions().getNTasksXP()) {
                    xpGained += getCampaignOptions().getTaskXP();
                    person.setNTasks(0);
                }
                // The person should have their acquisitions incremented
                person.incrementAcquisition();
            }
        } else {
            report = report + acquisition.failToFind();
            if (person != null && roll == 2
                    && target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
                xpGained += getCampaignOptions().getMistakeXP();
            }
        }
        if (xpGained > 0) {
            person.setXp(person.getXp() + xpGained);
            report += " (" + xpGained + "XP gained) ";
        }

        if (found) {
        	acquisition.decrementQuantity();
            MekHQ.triggerEvent(new AcquisitionEvent(acquisition));
        }
        addReport(report);
        return found;
    }

    public void mothball(Unit u) {
        Person tech = u.getTech();
        if (null == tech) {
            //uh-oh
            //TODO: report someting
            addReport("No tech assigned to the mothballing of " + u.getHyperlinkedName());
            return;
        }
        //don't allow overtime minutes for mothballing because its cheating
        //since you dont roll
        int minutes = Math.min(tech.getMinutesLeft(), u.getMothballTime());
        //check astech time
        if (!u.isSelfCrewed() && astechPoolMinutes < minutes * 6) {
            //uh-oh
            addReport("Not enough astechs to work on mothballing of " + u.getHyperlinkedName());
            return;
        }
        u.setMothballTime(u.getMothballTime() - minutes);
        String action = " mothballing ";
        if (u.isMothballed()) {
            action = " activating ";
        }
        String report = tech.getHyperlinkedFullTitle() + " spent " + minutes + " minutes" + action + u.getHyperlinkedName();
        if (!u.isMothballing()) {
            if (u.isMothballed()) {
                u.setMothballed(false);
                report += ". Activation complete.";
            } else {
                u.setMothballed(true);
                report += ". Mothballing complete.";
            }
        } else {
            report += ". " + u.getMothballTime() + " minutes remaining.";
        }
        tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
        if (!u.isSelfCrewed()) {
            astechPoolMinutes -= 6 * minutes;
        }
        addReport(report);
    }

    public void refit(Refit r) {
        Person tech = r.getUnit().getEngineer() == null?
                getPerson(r.getTeamId()) : r.getUnit().getEngineer();
        if (null == tech) {
            addReport("No tech is assigned to refit " + r.getOriginalEntity().getShortName() + ". Refit cancelled.");
            r.cancel();
            return;
                }
        TargetRoll target = getTargetFor(r, tech);
        // check that all parts have arrived
        if (!r.acquireParts()) {
            return;
                }
        String report = tech.getHyperlinkedFullTitle() + " works on " + r.getPartName();
        int minutes = r.getTimeLeft();
        // FIXME: Overtime?
        if (minutes > tech.getMinutesLeft()) {
            r.addTimeSpent(tech.getMinutesLeft());
            tech.setMinutesLeft(0);
            report = report + ", " + r.getTimeLeft() + " minutes left.";
                } else {
            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
            r.addTimeSpent(minutes);
            if (r.hasFailedCheck()) {
                report = report + ", " + r.succeed();
            } else {
                int roll;
                String wrongType = "";
                if (tech.isRightTechTypeFor(r)) {
                    roll = Compute.d6(2);
                } else {
                    roll = Utilities.roll3d6();
                    wrongType = " <b>Warning: wrong tech type for this refit.</b>";
                }
                report = report + ",  needs " + target.getValueAsString() + " and rolls " + roll + ": ";
                if (roll < target.getValue() && getCampaignOptions().useSupportEdge()
                        && tech.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_FAILED_REFIT)
                        && tech.getEdge() > 0) {
                    tech.setEdge(tech.getEdge() - 1);
                    if (tech.isRightTechTypeFor(r)) {
                        roll = Compute.d6(2);
                    } else {
                        roll = Utilities.roll3d6();
                    }
                    // This is needed to update the edge values of individual crewmen
                    if (tech.isEngineer()) {
                        tech.setEdgeUsed(tech.getEdgeUsed() - 1);
                    }
                    report += " <b>failed!</b> but uses Edge to reroll...getting a " + roll + ": ";
                }
                if (roll >= target.getValue()) {
                    report += r.succeed();
                } else {
                    report += r.fail(SkillType.EXP_GREEN);
                    // try to refit again in case the tech has any time left
                    if (!r.isBeingRefurbished()) {
                        refit(r);
                    }
                }
                report += wrongType;
                    }
                }
        MekHQ.triggerEvent(new PartWorkEvent(tech, r));
        addReport(report);
    }

    public Part fixWarehousePart(Part part, Person tech) {
        // get a new cloned part to work with and decrement original
        Part repairable = part.clone();
        part.decrementQuantity();
        fixPart(repairable, tech);
        if (!(repairable instanceof OmniPod)) {
            addPart(repairable, 0);
        }

        return repairable;
    }

    public void fixPart(IPartWork partWork, Person tech) {
        TargetRoll target = getTargetFor(partWork, tech);
        String report = "";
        String action = " fix ";

        // TODO: this should really be a method on the part
        if (partWork instanceof AmmoBin) {
            action = " reload ";
        }
        if (partWork.isSalvaging()) {
            action = " salvage ";
        }
        if (partWork instanceof MissingPart) {
            action = " replace ";
        }
        if (partWork instanceof MekLocation) {
            if (((MekLocation) partWork).isBlownOff()) {
                action = " re-attach ";
            } else if (((MekLocation) partWork).isBreached()) {
                action = " seal ";
            }
        }
        if ((partWork instanceof Armor) && !partWork.isSalvaging()) {
            if (!((Armor) partWork).isInSupply()) {
                report += "<b>Not enough armor remaining.  Task suspended.</b>";
                addReport(report);
                return;
            }
        }
        if ((partWork instanceof ProtomekArmor) && !partWork.isSalvaging()) {
            if (!((ProtomekArmor) partWork).isInSupply()) {
                report += "<b>Not enough Protomech armor remaining.  Task suspended.</b>";
                addReport(report);
                return;
            }
        }
        if ((partWork instanceof BaArmor) && !partWork.isSalvaging()) {
            if (!((BaArmor) partWork).isInSupply()) {
                report += "<b>Not enough BA armor remaining.  Task suspended.</b>";
                addReport(report);
                return;
            }
        }
        report += tech.getHyperlinkedFullTitle() + " attempts to" + action
                + partWork.getPartName();

        if (null != partWork.getUnit()) {
            report += " on " + partWork.getUnit().getName();
        }

        int minutes = partWork.getTimeLeft();
        int minutesUsed = minutes;
        boolean usedOvertime = false;
        if (minutes > tech.getMinutesLeft()) {
            minutes -= tech.getMinutesLeft();
            // check for overtime first
            if (isOvertimeAllowed() && minutes <= tech.getOvertimeLeft()) {
                // we are working overtime
                usedOvertime = true;
                tech.setMinutesLeft(0);
                tech.setOvertimeLeft(tech.getOvertimeLeft() - minutes);
            } else {
                // we need to finish the task tomorrow
                minutesUsed = tech.getMinutesLeft();
                if (isOvertimeAllowed()) {
                    minutesUsed += tech.getOvertimeLeft();
                    partWork.setWorkedOvertime(true);
                    usedOvertime = true;
                }
                partWork.addTimeSpent(minutesUsed);
                tech.setMinutesLeft(0);
                tech.setOvertimeLeft(0);
                int helpMod = getShorthandedMod(
                        getAvailableAstechs(minutesUsed, usedOvertime), false);
                if (null != partWork.getUnit()
                        && (partWork.getUnit().getEntity() instanceof Dropship
                                || partWork.getUnit().getEntity() instanceof Jumpship)) {
                    helpMod = 0;
                }
                if (partWork.getShorthandedMod() < helpMod) {
                    partWork.setShorthandedMod(helpMod);
                }
                partWork.setTeamId(tech.getId());
                partWork.reservePart();
                report += " - <b>Not enough time, the remainder of the task";
                if (null != partWork.getUnit()) {
                    report += " on " + partWork.getUnit().getName();
                }
                report += " will be finished tomorrow.</b>";
                MekHQ.triggerEvent(new PartWorkEvent(tech, partWork));
                addReport(report);
                return;
            }
        } else {
            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
        }
        int astechMinutesUsed = minutesUsed
                * getAvailableAstechs(minutesUsed, usedOvertime);
        if (astechPoolMinutes < astechMinutesUsed) {
            astechMinutesUsed -= astechPoolMinutes;
            astechPoolMinutes = 0;
            astechPoolOvertime -= astechMinutesUsed;
        } else {
            astechPoolMinutes -= astechMinutesUsed;
        }
        // check for the type
        int roll;
        String wrongType = "";
        if (tech.isRightTechTypeFor(partWork)) {
            roll = Compute.d6(2);
        } else {
            roll = Utilities.roll3d6();
            wrongType = " <b>Warning: wrong tech type for this repair.</b>";
        }
        report = report + ",  needs " + target.getValueAsString()
                + " and rolls " + roll + ":";
        int xpGained = 0;
        //if we fail and would break a part, here's a chance to use Edge for a reroll...
        if (getCampaignOptions().useSupportEdge() 
                && tech.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_BREAK_PART)
                && tech.getEdge() > 0
                && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
            if ((getCampaignOptions().isDestroyByMargin()
                    && getCampaignOptions().getDestroyMargin() <= (target.getValue() - roll))
                    || (!getCampaignOptions().isDestroyByMargin() && (tech.getExperienceLevel(false) == SkillType.EXP_ELITE //if an elite, primary tech and destroy by margin is NOT on
                                    || tech.getPrimaryRole() == Person.T_SPACE_CREW)) // For vessel crews
                            && roll < target.getValue()) {
                tech.setEdge(tech.getEdge() - 1);
                if (tech.isRightTechTypeFor(partWork)) {
                    roll = Compute.d6(2);
                } else {
                    roll = Utilities.roll3d6();
                }
                //This is needed to update the edge values of individual crewmen
                if (tech.isEngineer()) {
                    tech.setEdgeUsed(tech.getEdgeUsed() + 1);
                }
                report += " <b>failed!</b> and would destroy the part, but uses Edge to reroll...getting a " + roll + ":";                
            }
        }
        if (roll >= target.getValue()) {
            report = report + partWork.succeed();
            if (getCampaignOptions().payForRepairs()
                    && action == " fix "
                    && !(partWork instanceof Armor)) {
                int cost = (int) (((Part) partWork).getStickerPrice() * .2);
                report += "<br>Repairs cost " + cost + " C-bills worth of parts.";
                finances.debit(cost, Transaction.C_REPAIRS, "Repair of " + partWork.getPartName(), calendar.getTime());
            }
            if (roll == 12 && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                xpGained += getCampaignOptions().getSuccessXP();
            }
            if (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                tech.setNTasks(tech.getNTasks() + 1);
            }
            if (tech.getNTasks() >= getCampaignOptions().getNTasksXP()) {
                xpGained += getCampaignOptions().getTaskXP();
                tech.setNTasks(0);
            }
        } else {
            int modePenalty = partWork.getMode().expReduction;
            int effectiveSkillLvl = tech.getSkillForWorkingOn(partWork)
                    .getExperienceLevel() - modePenalty;
            if (getCampaignOptions().isDestroyByMargin()) {
                if (getCampaignOptions().getDestroyMargin() > (target.getValue() - roll)) {
                    // not destroyed - set the effective level as low as
                    // possible
                    effectiveSkillLvl = SkillType.EXP_ULTRA_GREEN;
                } else {
                    // destroyed - set the effective level to elite
                    effectiveSkillLvl = SkillType.EXP_ELITE;
                }
            }
            report = report + partWork.fail(effectiveSkillLvl);

            if (roll == 2 && target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
                xpGained += getCampaignOptions().getMistakeXP();
            }
        }
        if (xpGained > 0) {
            tech.setXp(tech.getXp() + xpGained);
            report += " (" + xpGained + "XP gained) ";
            if (tech.isEngineer()) {
                tech.setEngineerXp(xpGained);
            }
        }
        report += wrongType;
        partWork.resetTimeSpent();
        partWork.resetOvertime();
        partWork.setTeamId(null);
        partWork.cancelReservation();
        MekHQ.triggerEvent(new PartWorkEvent(tech, partWork));
        addReport(report);
    }

    /**
     * Parses news file and loads news items for the current year.
     */
    public void reloadNews() {
        news.loadNewsFor(getGameYear(), id.getLeastSignificantBits());
    }

    /**
     * Checks for a news item for the current date. If found, adds it to the daily report.
     */
    public void readNews() {
        //read the news
        DateTime now = Utilities.getDateTimeDay(calendar);
        for(NewsItem article : news.fetchNewsFor(now)) {
            addReport(article.getHeadlineForReport());
        }
        for(NewsItem article : Planets.getInstance().getPlanetaryNews(now)) {
            addReport(article.getHeadlineForReport());
        }
    }

    public int getDeploymentDeficit(AtBContract contract) {
        int total = -contract.getRequiredLances();
        int role = -Math.max(1, contract.getRequiredLances() / 2);
        for (Lance l : lances.values()) {
            if (l.getMissionId() == contract.getId() && l.getRole() != Lance.ROLE_UNASSIGNED) {
                total++;
                if (l.getRole() == contract.getRequiredLanceType()) {
                    role++;
                }
            }
        }

        if (total >= 0 && role >= 0) {
            return 0;
        }
        return Math.abs(Math.min(total, role));
    }
    
    private void processNewDayATBScenarios() {
        for (Mission m : getMissions()) {
            if (!m.isActive() || !(m instanceof AtBContract) || getDate().before(((Contract) m).getStartDate())) {
                continue;
            }
            /*
             * Situations like a delayed start or running out of funds during transit can
             * delay arrival until after the contract start. In that case, shift the
             * starting and ending dates before making any battle rolls. We check that the
             * unit is actually on route to the planet in case the user is using a custom
             * system for transport or splitting the unit, etc.
             */
            if (!getLocation().isOnPlanet() && 
                    getLocation().getJumpPath().getLastPlanet().getId().equals(m.getPlanetId())) {
                /*
                 * transitTime is measured in days; round up to the next whole day, then convert
                 * to milliseconds
                 */
                GregorianCalendar cal = (GregorianCalendar) calendar.clone();
                cal.add(Calendar.DATE, (int) Math.ceil(getLocation().getTransitTime()));
                ((AtBContract) m).getStartDate().setTime(cal.getTimeInMillis());
                cal.add(Calendar.MONTH, ((AtBContract) m).getLength());
                ((AtBContract) m).getEndingDate().setTime(cal.getTimeInMillis());
                addReport(
                        "The start and end dates of " + m.getName() + " have been shifted to reflect the current ETA.");
                continue;
            }
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                int deficit = getDeploymentDeficit((AtBContract) m);
                if (deficit > 0) {
                    ((AtBContract) m).addPlayerMinorBreaches(deficit);
                    addReport("Failure to meet " + m.getName() + " requirements resulted in " + deficit
                            + ((deficit == 1) ? " minor contract breach" : " minor contract breaches"));
                }
            }

            for (Scenario s : m.getScenarios()) {
                if (!s.isCurrent() || !(s instanceof AtBScenario)) {
                    continue;
                }
                if (s.getDate().before(calendar.getTime())) {
                    s.setStatus(Scenario.S_DEFEAT);
                    s.clearAllForcesAndPersonnel(this);
                    ((AtBContract) m).addPlayerMinorBreach();
                    addReport("Failure to deploy for " + s.getName()
                            + " resulted in defeat and a minor contract breach.");
                    ((AtBScenario) s).generateStub(this);
                }
            }
        }

        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            AtBScenarioFactory.createScenariosForNewWeek(this, true);
        }

        for (Mission m : getMissions()) {
            if (m.isActive() && m instanceof AtBContract && !((AtBContract) m).getStartDate().after(getDate())) {
                ((AtBContract) m).checkEvents(this);
            }
            /*
             * If there is a standard battle set for today, deploy the lance.
             */
            for (Scenario s : m.getScenarios()) {
                if (s.getDate() != null && s.getDate().equals(calendar.getTime())) {
                    int forceId = ((AtBScenario) s).getLanceForceId();
                    if (null != lances.get(forceId) && !forceIds.get(forceId).isDeployed()) {

                        // If any unit in the force is under repair, don't deploy the force
                        // Merely removing the unit from deployment would break with user expectation
                        boolean forceUnderRepair = false;
                        for (UUID uid : forceIds.get(forceId).getAllUnits()) {
                            Unit u = getUnit(uid);
                            if (null != u && u.isUnderRepair()) {
                                forceUnderRepair = true;
                                break;
                            }
                        }

                        if (!forceUnderRepair) {
                            forceIds.get(forceId).setScenarioId(s.getId());
                            s.addForces(forceId);
                            for (UUID uid : forceIds.get(forceId).getAllUnits()) {
                                Unit u = getUnit(uid);
                                if (null != u) {
                                    u.setScenarioId(s.getId());
                                }
                            }
                            MekHQ.triggerEvent(new DeploymentChangedEvent(forceIds.get(forceId), s));
                        }
                    }
                }
            }
        }
    }

    private void processNewDayATBFatigue() {
        boolean inContract = false;
        for (Mission m : getMissions()) {
            if (!m.isActive() || !(m instanceof AtBContract) || getDate().before(((Contract) m).getStartDate())) {
                continue;
            }
            switch (((AtBContract) m).getMissionType()) {
                case AtBContract.MT_GARRISONDUTY:
                case AtBContract.MT_SECURITYDUTY:
                case AtBContract.MT_CADREDUTY:
                    fatigueLevel -= 1;
                    break;
                case AtBContract.MT_RIOTDUTY:
                case AtBContract.MT_GUERRILLAWARFARE:
                case AtBContract.MT_PIRATEHUNTING:
                    fatigueLevel += 1;
                    break;
                case AtBContract.MT_RELIEFDUTY:
                case AtBContract.MT_PLANETARYASSAULT:
                    fatigueLevel += 2;
                    break;
                case AtBContract.MT_DIVERSIONARYRAID:
                case AtBContract.MT_EXTRACTIONRAID:
                case AtBContract.MT_RECONRAID:
                case AtBContract.MT_OBJECTIVERAID:
                    fatigueLevel += 3;
                    break;
            }
            inContract = true;
        }
        if (!inContract && location.isOnPlanet()) {
            fatigueLevel -= 2;
        }
        fatigueLevel = Math.max(fatigueLevel, 0);
    }

    private void processNewDayATB() {
        contractMarket.generateContractOffers(this);
        unitMarket.generateUnitOffers(this);

        if (shipSearchExpiration != null && !shipSearchExpiration.after(calendar)) {
            shipSearchExpiration = null;
            if (shipSearchResult != null) {
                addReport("Opportunity for purchase of " + shipSearchResult + " has expired.");
                shipSearchResult = null;
            }
        }

        if (getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            processShipSearch();
        }

        // Add or remove dependents
        if (calendar.get(Calendar.DAY_OF_YEAR) == 1) {
            int numPersonnel = 0;
            ArrayList<Person> dependents = new ArrayList<Person>();
            for (Person p : getPersonnel()) {
                if (p.isActive()) {
                    numPersonnel++;
                    if (p.isDependent()) {
                        dependents.add(p);
                    }
                }
            }
            int roll = Compute.d6(2) + getUnitRatingMod() - 2;
            if (roll < 2)
                roll = 2;
            if (roll > 12)
                roll = 12;
            int change = numPersonnel * (roll - 5) / 100;
            while (change < 0 && dependents.size() > 0) {
                removePerson(Utilities.getRandomItem(dependents).getId());
                change++;
            }
            for (int i = 0; i < change; i++) {
                Person p = newPerson(Person.T_ASTECH);
                p.setDependent(true);
                p.setId(UUID.randomUUID());
                addPersonWithoutId(p, true);
            }
        }

        if (calendar.get(Calendar.DAY_OF_MONTH) == 1) {
            /*
             * First of the month; roll morale, track unit fatigue.
             */

            IUnitRating rating = getUnitRating();
            rating.reInitialize();

            for (Mission m : getMissions()) {
                if (m.isActive() && m instanceof AtBContract && !((AtBContract) m).getStartDate().after(getDate())) {
                    ((AtBContract) m).checkMorale(calendar, getUnitRatingMod());
                    addReport("Enemy morale is now " + ((AtBContract) m).getMoraleLevelName() + " on contract "
                            + m.getName());
                }
            }

            // Account for fatigue
            if (getCampaignOptions().getTrackUnitFatigue()) {
                processNewDayATBFatigue();
            }
        }

        processNewDayATBScenarios();
    }

    public void processNewDayPersonnel() {
        ArrayList<Person> babies = new ArrayList<Person>();
        for (Person p : getPersonnel()) {
            if (!p.isActive()) {
                continue;
            }

            // Procreation
            if (p.isFemale()) {
                if (p.isPregnant()) {
                    if (getCampaignOptions().useUnofficialProcreation()) {
                        if (getCalendar().compareTo((p.getDueDate())) == 0) {
                            babies.addAll(p.birth());
                        }
                    } else {
                        p.setDueDate(null);
                    }
                } else if (getCampaignOptions().useUnofficialProcreation()) {
                    p.procreate();
                }
            }

            p.resetMinutesLeft();
            // Reset acquisitions made to 0
            p.setAcquisition(0);
            if (p.needsFixing() && !getCampaignOptions().useAdvancedMedical()) {
                p.decrementDaysToWaitForHealing();
                Person doctor = getPerson(p.getDoctorId());
                if (null != doctor && doctor.isDoctor()) {
                    if (p.getDaysToWaitForHealing() <= 0) {
                        addReport(healPerson(p, doctor));
                    }
                } else if (p.checkNaturalHealing(15)) {
                    addReport(p.getHyperlinkedFullTitle() + " heals naturally!");
                    Unit u = getUnit(p.getUnitId());
                    if (null != u) {
                        u.resetPilotAndEntity();
                    }
                }
            }
            // TODO Advanced Medical needs to go away from here later on
            if (getCampaignOptions().useAdvancedMedical()) {
                InjuryUtil.resolveDailyHealing(this, p);
                Unit u = getUnit(p.getUnitId());
                if (null != u) {
                    u.resetPilotAndEntity();
                }
            }

            if (getCampaignOptions().getIdleXP() > 0 && calendar.get(Calendar.DAY_OF_MONTH) == 1 && p.isActive()
                    && !p.isPrisoner()) { // Prisoners no
                                          // XP, Bondsmen
                                          // yes xp
                p.setIdleMonths(p.getIdleMonths() + 1);
                if (p.getIdleMonths() >= getCampaignOptions().getMonthsIdleXP()) {
                    if (Compute.d6(2) >= getCampaignOptions().getTargetIdleXP()) {
                        p.setXp(p.getXp() + getCampaignOptions().getIdleXP());
                        addReport(p.getHyperlinkedFullTitle() + " has gained " + getCampaignOptions().getIdleXP()
                                + " XP");
                    }
                    p.setIdleMonths(0);
                }
            }

        }

        for (Person baby : babies) {
            addPersonWithoutId(baby, false);
        }
    }

    public void processNewDayUnits() {
        // need to loop through units twice, the first time to do all maintenance and
        // the second
        // time to do whatever else. Otherwise, maintenance minutes might get sucked up
        // by other
        // stuff. This is also a good place to ensure that a unit's engineer gets reset
        // and updated.
        for (Unit u : getUnits()) {
            u.resetEngineer();
            if (null != u.getEngineer()) {
                u.getEngineer().resetMinutesLeft();
            }

            // do maintenance checks
            doMaintenance(u);
        }

        // need to check for assigned tasks in two steps to avoid
        // concurrent mod problems
        ArrayList<Integer> assignedPartIds = new ArrayList<Integer>();
        ArrayList<Integer> arrivedPartIds = new ArrayList<Integer>();
        for (Part part : getParts()) {
            if (part instanceof Refit) {
                continue;
            }
            if (part.getTeamId() != null) {
                assignedPartIds.add(part.getId());
            }
            if (part.checkArrival()) {
                arrivedPartIds.add(part.getId());
            }
        }

        // arrive parts before attempting refit or parts will not get reserved that day
        for (int pid : arrivedPartIds) {
            Part part = getPart(pid);
            if (null != part) {
                arrivePart(part);
            }
        }

        // finish up any overnight assigned tasks
        for (int pid : assignedPartIds) {
            Part part = getPart(pid);
            if (null != part) {
                Person tech = null;
                if (part.getUnit() != null && part.getUnit().getEngineer() != null) {
                    tech = part.getUnit().getEngineer();
                } else {
                    tech = getPerson(part.getTeamId());
                }
                if (null != tech) {
                    if (null != tech.getSkillForWorkingOn(part)) {
                        fixPart(part, tech);
                    } else {
                        addReport(String.format(
                                "%s looks at %s, recalls his total lack of skill for working with such technology, then slowly puts the tools down before anybody gets hurt.",
                                tech.getHyperlinkedFullTitle(), part.getName()));
                        part.setTeamId(null);
                    }
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Could not find tech for part: " + part.getName() + " on unit: "
                                    + part.getUnit().getHyperlinkedName(),
                            "Invalid Auto-continue", JOptionPane.ERROR_MESSAGE);
                }
                // check to see if this part can now be combined with other
                // spare parts
                if (part.isSpare()) {
                    Part spare = checkForExistingSparePart(part);
                    if (null != spare) {
                        spare.incrementQuantity();
                        removePart(part);
                    }
                }
            }
        }

        // ok now we can check for other stuff we might need to do to units
        for (Unit u : getUnits()) {
            if (u.isRefitting()) {
                refit(u.getRefit());
            }
            if (u.isMothballing()) {
                mothball(u);
            }
            if (!u.isPresent()) {
                u.checkArrival();
            }
        }
    }

    /** @return <code>true</code> if the new day arrived */
    public boolean newDay() {
        if(MekHQ.triggerEvent(new DayEndingEvent(this))) {
            return false;
        }

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        currentReport.clear();
        currentReportHTML = "";
        newReports.clear();
        beginReport("<b>" + getDateAsString() + "</b>");

        if (calendar.get(Calendar.DAY_OF_YEAR) == 1) {
            reloadNews();
        }

        // Ensure that the MegaMek year GameOption matches the campaign year
        if (gameOptions.intOption("year") != getGameYear()) {
            gameOptions.getOption("year").setValue(getGameYear());
        }

        readNews();

        location.newDay(this);

        // Manage the personnel market
        personnelMarket.generatePersonnelForDay(this);

        if (campaignOptions.getUseAtB()) {
            processNewDayATB();
        }

        processNewDayPersonnel();

        resetAstechMinutes();

        shoppingList = goShopping(shoppingList);

        processNewDayUnits();

        // check for anything in finances
        finances.newDay(this);

        MekHQ.triggerEvent(new NewDayEvent(this));
        return true;
    }

    public ArrayList<Contract> getActiveContracts() {
        ArrayList<Contract> active = new ArrayList<Contract>();
        for (Mission m : getMissions()) {
            if (!(m instanceof Contract)) {
                continue;
            }
            Contract c = (Contract) m;
            if (c.isActive()
                    && !getCalendar().getTime().after(c.getEndingDate())
                    && !getCalendar().getTime().before(c.getStartDate())) {
                active.add(c);
            }
        }
        return active;
    }

    public Person getFlaggedCommander() {
        for (Person p : getPersonnel()) {
            if (p.isCommander()) {
                return p;
            }
        }
        return null;
    }

    public long getPayRoll() {
        return getPayRoll(false);
    }

    public long getPayRoll(boolean noInfantry) {
        if(!campaignOptions.payForSalaries()) return 0;

        return getTheoreticalPayroll(noInfantry);
    }

    private long getTheoreticalPayroll(boolean noInfantry){
        long salaries = 0;
        for (Person p : getPersonnel()) {
            // Optionized infantry (Unofficial)
            if (noInfantry && p.getPrimaryRole() == Person.T_INFANTRY) {
                continue;
            }

            if (p.isActive() && !p.isDependent()
                    && !(p.isPrisoner() || p.isBondsman())) {
                salaries += p.getSalary();
            }
        }
        // add in astechs from the astech pool
        // we will assume Mech Tech * able-bodied * enlisted (changed from vee mechanic)
        // 800 * 0.5 * 0.6 = 240
        salaries += (240 * astechPool);
        salaries += (320 * medicPool);
        return salaries;
    }

    public long getMaintenanceCosts() {
        long costs = 0;
        if(campaignOptions.payForMaintain()) {
            for (Map.Entry<UUID, Unit> mu : units.entrySet()) {
                Unit u = mu.getValue();
                if (u.requiresMaintenance() && null != u.getTech()) {
                    costs += u.getMaintenanceCost();
                }
            }
        }
        return costs;
    }

    public long getWeeklyMaintenanceCosts() {
        long costs = 0;
        for (Map.Entry<UUID, Unit> u : units.entrySet()) {
            costs += u.getValue().getWeeklyMaintenanceCost();
        }
        return costs;
    }

    public long getOverheadExpenses() {
        if(!campaignOptions.payForOverhead()) return 0;

        return (long) (getTheoreticalPayroll(false) * 0.05);
    }

    public void removeUnit(UUID id) {
        Unit unit = getUnit(id);

        // remove all parts for this unit as well
        for (Part p : unit.getParts()) {
            removePart(p);
        }

        // remove any personnel from this unit
        for (Person p : unit.getCrew()) {
            unit.remove(p, true);
        }

        Person tech = unit.getTech();
        if (null != tech) {
            unit.remove(tech, true);
        }

        // remove unit from any forces
        removeUnitFromForce(unit);

        // finally remove the unit
        units.remove(unit.getId());
        checkDuplicateNamesDuringDelete(unit.getEntity());
        addReport(unit.getName() + " has been removed from the unit roster.");
        MekHQ.triggerEvent(new UnitRemovedEvent(unit));
    }

    public void removePerson(UUID id) {
        removePerson(id, true);
    }

    public void removePerson(UUID id, boolean log) {
        Person person = getPerson(id);

        if (person == null) {
            return;
        }

        Unit u = getUnit(person.getUnitId());
        if (null != u) {
            u.remove(person, true);
        }
        removeAllPatientsFor(person);
        removeAllTechJobsFor(person);

        if (log) {
            addReport(person.getFullTitle()
                    + " has been removed from the personnel roster.");
        }

        personnel.remove(id);
        if (person.getPrimaryRole() == Person.T_ASTECH) {
            astechPoolMinutes = Math.max(0, astechPoolMinutes - 480);
            astechPoolOvertime = Math.max(0, astechPoolOvertime - 240);
        }
        if (person.getSecondaryRole() == Person.T_ASTECH) {
            astechPoolMinutes = Math.max(0, astechPoolMinutes - 240);
            astechPoolOvertime = Math.max(0, astechPoolOvertime - 120);
        }
        MekHQ.triggerEvent(new PersonRemovedEvent(person));        
    }

    public void awardTrainingXP(Lance l) {
        awardTrainingXPByMaximumRole(l);
    }

    /**
     * Awards XP to the lance based on the maximum experience level of its
     * commanding officer and the minumum experience level of the unit's
     * members.
     * @param l The {@link Lance} to calculate XP to award for training.
     */
    private void awardTrainingXPByMaximumRole(Lance l) {
        for (UUID trainerId : forceIds.get(l.getForceId()).getAllUnits()) {
            Person commander = getUnit(trainerId).getCommander();
            // AtB 2.31: Training lance – needs a officer with Veteran skill levels
            //           and adds 1xp point to every Green skilled unit.
            if (commander != null && commander.getRank().isOfficer()) {
                // Take the maximum of the commander's Primary and Secondary Role
                // experience to calculate their experience level...
                int commanderExperience = Math.max(commander.getExperienceLevel(false),
                        commander.getExperienceLevel(true));
                if (commanderExperience > SkillType.EXP_REGULAR) {
                    // ...and if the commander is better than a veteran, find all of
                    // the personnel under their command...
                    for (UUID traineeId : forceIds.get(l.getForceId()).getAllUnits()) {
                        for (Person p : getUnit(traineeId).getCrew()) {
                            if (p == commander) {
                                continue;
                            }
                            // ...and if their weakest role is Green or Ultra-Green
                            int experienceLevel = Math.min(p.getExperienceLevel(false),
                                    p.getSecondaryRole() != Person.T_NONE
                                            ? p.getExperienceLevel(true)
                                            : SkillType.EXP_ELITE);
                            if (experienceLevel >= 0 && experienceLevel < SkillType.EXP_REGULAR) {
                                // ...add one XP.
                                p.setXp(p.getXp() + 1);
                                addReport(p.getHyperlinkedName() + " has gained 1 XP from training.");
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    public void removeAllPatientsFor(Person doctor) {
        for (Person p : getPersonnel()) {
            if (null != p.getDoctorId()
                    && p.getDoctorId().equals(doctor.getId())) {
                p.setDoctorId(null, getCampaignOptions()
                        .getNaturalHealingWaitingPeriod());
            }
        }
    }

    public void removeAllTechJobsFor(Person tech) {
        if (tech == null || tech.getId() == null) {
            return;
        }
        for (Map.Entry<UUID, Unit> mu : units.entrySet()) {
            Unit u = mu.getValue();
            if (tech.getId().equals(u.getTechId())) {
                u.removeTech();
            }
            if (u.getRefit() != null && tech.getId().equals(u.getRefit().getTeamId())) {
                u.getRefit().setTeamId(null);
            }
        }
        for (Part p : getParts()) {
            if (tech.getId().equals(p.getTeamId())) {
                p.setTeamId(null);
            }
        }
        for (Force f : forceIds.values()) {
            if (tech.getId().equals(f.getTechID())) {
                f.setTechID(null);
            }
        }
    }

    public void removeScenario(int id) {
        Scenario scenario = getScenario(id);
        scenario.clearAllForcesAndPersonnel(this);
        Mission mission = getMission(scenario.getMissionId());
        if (null != mission) {
            mission.removeScenario(scenario.getId());
        }
        scenarios.remove(Integer.valueOf(id));
        MekHQ.triggerEvent(new ScenarioChangedEvent(scenario));
    }

    public void removeMission(int id) {
        Mission mission = getMission(id);

        // Loop through scenarios here! We need to remove them as well.
        if (null != mission) {
            for (Scenario scenario : mission.getScenarios()) {
                scenario.clearAllForcesAndPersonnel(this);
                scenarios.remove(scenario.getId());
            }
            mission.clearScenarios();
        }

        missions.remove(Integer.valueOf(id));
    }

    public void removePart(Part part) {
        if (null != part.getUnit() && part.getUnit() instanceof TestUnit) {
            // if this is a test unit, then we won't remove the part because its not there
            return;
        }
        parts.remove(Integer.valueOf(part.getId()));
        //remove child parts as well
        for(int childId : part.getChildPartIds()) {
            Part childPart = getPart(childId);
            if (null != childPart) {
                removePart(childPart);
            }
        }
        MekHQ.triggerEvent(new PartRemovedEvent(part));
    }

    public void removeKill(Kill k) {
        kills.remove(k);
    }

    public void removeForce(Force force) {
        int fid = force.getId();
        forceIds.remove(Integer.valueOf(fid));
        // clear forceIds of all personnel with this force
        for (UUID uid : force.getUnits()) {
            Unit u = getUnit(uid);
            if (null == u) {
                continue;
            }
            if (u.getForceId() == fid) {
                u.setForceId(-1);
                if (force.isDeployed()) {
                    u.setScenarioId(-1);
                }
            }
        }
        MekHQ.triggerEvent(new OrganizationChangedEvent(force));
        // also remove this force's id from any scenarios
        if (force.isDeployed()) {
            Scenario s = getScenario(force.getScenarioId());
            s.removeForce(fid);
        }

        if (campaignOptions.getUseAtB()) {
            lances.remove(fid);
        }

        if (null != force.getParentForce()) {
            force.getParentForce().removeSubForce(fid);
        }
        ArrayList<Force> subs = new ArrayList<Force>();
        for (Force sub : force.getSubForces()) {
            subs.add(sub);
        }
        for (Force sub : subs) {
            removeForce(sub);
            MekHQ.triggerEvent(new OrganizationChangedEvent(sub));
        }
    }

    public void removeUnitFromForce(Unit u) {
        Force force = getForce(u.getForceId());
        if (null != force) {
            force.removeUnit(u.getId());
            u.setForceId(Force.FORCE_NONE);
            u.setScenarioId(-1);
            if (u.getEntity().hasC3i()
                    && u.getEntity().calculateFreeC3Nodes() < 5) {
                Vector<Unit> removedUnits = new Vector<Unit>();
                removedUnits.add(u);
                removeUnitsFromNetwork(removedUnits);
                u.getEntity().setC3MasterIsUUIDAsString(null);
                u.getEntity().setC3Master(null, true);
                refreshNetworks();
            }
            if (u.getEntity().hasC3M()) {
                removeUnitsFromC3Master(u);
                u.getEntity().setC3MasterIsUUIDAsString(null);
                u.getEntity().setC3Master(null, true);
            }


            if (campaignOptions.getUseAtB() && force.getUnits().size() == 0) {
                lances.remove(force.getId());
            }
        }
    }

    public Force getForceFor(Unit u) {
        return getForce(u.getForceId());
    }

    public Force getForceFor(Person p) {
        Unit u = getUnit(p.getUnitId());
        if (null == u) {
            return null;
        } else {
            return getForceFor(u);
        }
    }

    public String getDateAsString() {
        return dateFormat.format(calendar.getTime());
    }

    public String getShortDateAsString() {
        return shortDateFormat.format(calendar.getTime());
    }

    public void restore() {
        // if we fail to restore equipment parts then remove them
        // and possibly re-initialize and diagnose unit
        ArrayList<Part> partsToRemove = new ArrayList<Part>();
        ArrayList<UUID> unitsToCheck = new ArrayList<UUID>();

        for (Part part : getParts()) {
            if (part instanceof EquipmentPart) {
                ((EquipmentPart) part).restore();
                if(null == ((EquipmentPart) part).getType()) {
                    partsToRemove.add(part);
                }
            }
            if (part instanceof MissingEquipmentPart) {
                ((MissingEquipmentPart) part).restore();
                if(null == ((MissingEquipmentPart) part).getType()) {
                    partsToRemove.add(part);
                }
            }
        }

        for(Part remove : partsToRemove) {
            if (null != remove.getUnitId() && !unitsToCheck.contains(remove.getUnitId())) {
                unitsToCheck.add(remove.getUnitId());
            }
            removePart(remove);
        }

        for (Unit unit : getUnits()) {
            if (null != unit.getEntity()) {
                unit.getEntity().setOwner(player);
                unit.getEntity().setGame(game);
                unit.getEntity().restore();
            }
        }

        for(UUID uid : unitsToCheck) {
            Unit u = getUnit(uid);
            if (null != u) {
                u.initializeParts(true);
                u.runDiagnostic(false);
            }
        }

        shoppingList.restore();

        if (getCampaignOptions().getUseAtB()) {
            RandomFactionGenerator.getInstance().startup(this);
            while (!RandomUnitGenerator.getInstance().isInitialized()) {
                //Sleep for up to one second.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignore) {

                }
            }
            RandomNameGenerator.getInstance();
            RandomFactionGenerator.getInstance().startup(this);
        }
    }

    public boolean isOvertimeAllowed() {
        return overtime;
    }

    public void setOvertime(boolean b) {
        this.overtime = b;
        MekHQ.triggerEvent(new OvertimeModeEvent(b));
    }

    public boolean isGM() {
        return gmMode;
    }

    public void setGMMode(boolean b) {
        this.gmMode = b;
        MekHQ.triggerEvent(new GMModeEvent(b));
    }

    public Faction getFaction() {
        return Faction.getFaction(factionCode);
    }

    public String getFactionName() {
        return getFaction().getFullName(getGameYear());
    }

    public void setFactionCode(String i) {
        this.factionCode = i;
        updateTechFactionCode();
    }

    public String getFactionCode() {
        return factionCode;
    }

    public String getRetainerEmployerCode() {
        return retainerEmployerCode;
    }

    public void setRetainerEmployerCode(String code) {
        retainerEmployerCode = code;
    }

    private void addInMemoryLogHistory(LogEntry le) {
        if (inMemoryLogHistory.size() != 0) {
            long diff = le.getDate().getTime() - inMemoryLogHistory.get(0).getDate().getTime();
            while ((diff / (1000 * 60 * 60 * 24)) > HistoricalDailyReportDialog.MAX_DAYS_HISTORY) {
                //we've hit the max size for the in-memory based on the UI display limit
                //prune the oldest entry
                inMemoryLogHistory.remove(0);
                diff = le.getDate().getTime() - inMemoryLogHistory.get(0).getDate().getTime();
            }
        }
        inMemoryLogHistory.add(le);
    }

    /**
     * Starts a new day for the daily log
     * @param r - the report String
     */
    public void beginReport(String r) {
        if (this.getCampaignOptions().historicalDailyLog()) {
            //add the new items to our in-memory cache
            addInMemoryLogHistory(new LogEntry(getDate(), ""));
        }
        addReportInternal(r);
    }

    /**
     * Adds a report to the daily log
     * @param r - the report String
     */
    public void addReport(String r) {
        if (this.getCampaignOptions().historicalDailyLog()) {
            addInMemoryLogHistory(new LogEntry(getDate(), r));
        }
        addReportInternal(r);
    }

    private void addReportInternal(String r) {
        currentReport.add(r);
        if( currentReportHTML.length() > 0 ) {
            currentReportHTML = currentReportHTML + REPORT_LINEBREAK + r;
            newReports.add(REPORT_LINEBREAK);
            newReports.add(r);
        } else {
            currentReportHTML = r;
            newReports.add(r);
        }
        MekHQ.triggerEvent(new ReportEvent(this, r));
    }

    public void addReports(ArrayList<String> reports) {
        for (String r : reports) {
            addReport(r);
        }
    }

    public void setCamoCategory(String name) {
        camoCategory = name;
    }

    public String getCamoCategory() {
        return camoCategory;
    }

    public void setCamoFileName(String name) {
        camoFileName = name;
    }

    public String getCamoFileName() {
        return camoFileName;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int index) {
        colorIndex = index;
    }

    public ArrayList<Part> getSpareParts() {
        ArrayList<Part> spares = new ArrayList<Part>();
        for (Part part : getParts()) {
            if (part.isSpare()) {
                spares.add(part);
            }
        }
        return spares;
    }

    public void addFunds(long quantity) {
        addFunds(quantity, "Rich Uncle", Transaction.C_MISC);
    }

    public void addFunds(long quantity, String description, int category) {
        if (description == null || description.isEmpty()) {
            description = "Rich Uncle";
        }
        if (category == -1) {
            category = Transaction.C_MISC;
        }
        finances.credit(quantity, category, description, calendar.getTime());
        NumberFormat numberFormat = DecimalFormat.getIntegerInstance();
        String quantityString = numberFormat.format(quantity);
        addReport("Funds added : " + quantityString + " (" + description + ")");
    }

    public boolean hasEnoughFunds(long cost) {
        return getFunds() >= cost;
    }

    public boolean buyUnit(Entity en, int days) {
        long cost = new Unit(en, this).getBuyCost();
        if (campaignOptions.payForUnits()) {
            if (finances.debit(cost, Transaction.C_UNIT,
                    "Purchased " + en.getShortName(), calendar.getTime())) {
                addUnit(en, false, days);
                return true;
            } else {
                return false;
            }
        } else {
            addUnit(en, false, days);
            return true;
        }
    }

    public void sellUnit(UUID id) {
        Unit unit = getUnit(id);
        long sellValue = unit.getSellValue();
        finances.credit(sellValue, Transaction.C_UNIT_SALE,
                "Sale of " + unit.getName(), calendar.getTime());
        removeUnit(id);
        MekHQ.triggerEvent(new UnitRemovedEvent(unit));
    }

    public void sellPart(Part part, int quantity) {
        if (part instanceof AmmoStorage) {
            sellAmmo((AmmoStorage) part, quantity);
            return;
        }
        if (part instanceof Armor) {
            sellArmor((Armor) part, quantity);
            return;
        }
        long cost = part.getActualValue() * quantity;
        String plural = "";
        if (quantity > 1) {
            plural = "s";
        }
        finances.credit(cost, Transaction.C_EQUIP_SALE, "Sale of " + quantity
                + " " + part.getName() + plural, calendar.getTime());
        while (quantity > 0 && part.getQuantity() > 0) {
            part.decrementQuantity();
            quantity--;
        }
        MekHQ.triggerEvent(new PartRemovedEvent(part));
    }

    public void sellAmmo(AmmoStorage ammo, int shots) {
        shots = Math.min(shots, ammo.getShots());
        boolean sellingAllAmmo = shots == ammo.getShots();
        long cost = (long) (ammo.getActualValue() * ((double) shots / ammo.getShots()));
        finances.credit(cost, Transaction.C_EQUIP_SALE, "Sale of " + shots
                + " " + ammo.getName(), calendar.getTime());
        if (sellingAllAmmo) {
            ammo.decrementQuantity();
        } else {
            ammo.changeShots(-1 * shots);
        }
        MekHQ.triggerEvent(new PartRemovedEvent(ammo));
    }

    public void sellArmor(Armor armor, int points) {
        points = Math.min(points, armor.getAmount());
        boolean sellingAllArmor = points == armor.getAmount();
        double proportion = ((double) points / armor.getAmount());
        if(sellingAllArmor) {
            // to avoid rounding error
            proportion = 1.0;
        }
        long cost = (long) (armor.getActualValue() * proportion);
        finances.credit(cost, Transaction.C_EQUIP_SALE, "Sale of " + points
                + " " + armor.getName(), calendar.getTime());
        if (sellingAllArmor) {
            armor.decrementQuantity();
        } else {
            armor.changeAmountAvailable(-1 * points);
        }
        MekHQ.triggerEvent(new PartRemovedEvent(armor));
    }

    public void depodPart(Part part, int quantity) {
        Part unpodded = part.clone();
        unpodded.setOmniPodded(false);
        OmniPod pod = new OmniPod(unpodded, this);
        while (quantity > 0 && part.getQuantity() > 0) {
            addPart(unpodded.clone(), 0);
            addPart(pod.clone(), 0);
            part.decrementQuantity();
            quantity--;
        }
        MekHQ.triggerEvent(new PartRemovedEvent(part));
        MekHQ.triggerEvent(new PartRemovedEvent(pod));
        MekHQ.triggerEvent(new PartRemovedEvent(unpodded));
    }

    public boolean buyRefurbishment(Part part) {
        if (getCampaignOptions().payForParts()) {
            if (finances.debit(part.getStickerPrice(), Transaction.C_EQUIP, "Purchase of " + part.getName(), calendar.getTime())) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public boolean buyPart(Part part, int transitDays) {
        return buyPart(part, 1, transitDays);
    }

    public boolean buyPart(Part part, double multiplier, int transitDays) {
        if (getCampaignOptions().payForParts()) {
            if (finances.debit((long) (multiplier * part.getStickerPrice()),
                    Transaction.C_EQUIP, "Purchase of " + part.getName(), calendar.getTime())) {
                if (part instanceof Refit) {
                    ((Refit) part).addRefitKitParts(transitDays);
                } else {
                    addPart(part, transitDays);
                }
                MekHQ.triggerEvent(new PartNewEvent(part));
                return true;
            } else {
                return false;
            }
        } else {
            if (part instanceof Refit) {
                ((Refit) part).addRefitKitParts(transitDays);
            } else {
                addPart(part, transitDays);
            }
            MekHQ.triggerEvent(new PartNewEvent(part));
            return true;
        }
    }

    public static Entity getBrandNewUndamagedEntity(String entityShortName) {
        MechSummary mechSummary = MechSummaryCache.getInstance().getMech(
                entityShortName);
        if (mechSummary == null) {
            return null;
        }

        MechFileParser mechFileParser = null;
        try {
            mechFileParser = new MechFileParser(mechSummary.getSourceFile());
        } catch (EntityLoadingException ex) {
            MekHQ.getLogger().error(Campaign.class, "getBrandNewUndamagedEntity(String)", ex);
        }
        if (mechFileParser == null) {
            return null;
        }

        return mechFileParser.getEntity();
    }

    public CampaignOptions getCampaignOptions() {
        return campaignOptions;
    }

    public void setCampaignOptions(CampaignOptions options) {
        campaignOptions = options;
    }


    public void writeToXml(PrintWriter pw1) {
        // File header
        pw1.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        ResourceBundle resourceMap = ResourceBundle
                .getBundle("mekhq.resources.MekHQ");
        // Start the XML root.
        pw1.println("<campaign version=\""
                + resourceMap.getString("Application.version") + "\">");

        // Basic Campaign Info
        pw1.println("\t<info>");

        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "id", id.toString());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "name", name);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "faction", factionCode);
        if (retainerEmployerCode != null) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "retainerEmployerCode", retainerEmployerCode);
        }

        // Ranks
        ranks.writeToXml(pw1, 3);

        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "nameGen",
                rng.getChosenFaction());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "percentFemale",
                rng.getPercentFemale());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "overtime", overtime);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "gmMode", gmMode);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "showOverview", app.getCampaigngui()
                .hasTab(GuiTabType.OVERVIEW));
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "astechPool", astechPool);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "astechPoolMinutes",
                astechPoolMinutes);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "astechPoolOvertime",
                astechPoolOvertime);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "medicPool", medicPool);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "camoCategory", camoCategory);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "camoFileName", camoFileName);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "colorIndex", colorIndex);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastTeamId", lastTeamId);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastPartId", lastPartId);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastForceId", lastForceId);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastMissionId", lastMissionId);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastScenarioId", lastScenarioId);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "calendar",
                df.format(calendar.getTime()));
        MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "fatigueLevel", fatigueLevel);
        {
            pw1.println("\t\t<nameGen>");
            pw1.print("\t\t\t<faction>");
            pw1.print(MekHqXmlUtil.escape(rng.getChosenFaction()));
            pw1.println("</faction>");
            pw1.print("\t\t\t<percentFemale>");
            pw1.print(rng.getPercentFemale());
            pw1.println("</percentFemale>");
            pw1.println("\t\t</nameGen>");
        }
        {
            pw1.println("\t\t<currentReport>");

            for (int x = 0; x < currentReport.size(); x++) {
                pw1.print("\t\t\t<reportLine><![CDATA[");
                pw1.print(currentReport.get(x));
                pw1.println("]]></reportLine>");
            }

            pw1.println("\t\t</currentReport>");
        }

        pw1.println("\t</info>");

        // Campaign Options
        // private CampaignOptions campaignOptions = new CampaignOptions();
        if (getCampaignOptions() != null) {
            getCampaignOptions().writeToXml(pw1, 1);
        }

        // Lists of objects:
        writeMapToXml(pw1, 1, "units", units); // Units
        writeMapToXml(pw1, 1, "personnel", personnel); // Personnel
        writeMapToXml(pw1, 1, "ancestors", ancestors); // Ancestry trees
        writeMapToXml(pw1, 1, "missions", missions); // Missions
        // the forces structure is hierarchical, but that should be handled
        // internally
        // from with writeToXML function for Force
        pw1.println("\t<forces>");
        forces.writeToXml(pw1, 2);
        pw1.println("\t</forces>");
        finances.writeToXml(pw1, 1);
        location.writeToXml(pw1, 1);
        shoppingList.writeToXml(pw1, 1);
        pw1.println("\t<kills>");
        for (Kill k : kills) {
            k.writeToXml(pw1, 2);
        }
        pw1.println("\t</kills>");
        pw1.println("\t<skillTypes>");
        for (String name : SkillType.skillList) {
            SkillType type = SkillType.getType(name);
            if (null != type) {
                type.writeToXml(pw1, 2);
            }
        }
        pw1.println("\t</skillTypes>");
        pw1.println("\t<specialAbilities>");
        for(String key : SpecialAbility.getAllSpecialAbilities().keySet()) {
            SpecialAbility.getAbility(key).writeToXml(pw1, 2);
        }
        pw1.println("\t</specialAbilities>");
        rskillPrefs.writeToXml(pw1, 1);
        // parts is the biggest so it goes last
        writeMapToXml(pw1, 1, "parts", parts); // Parts

        writeGameOptions(pw1);

        // Personnel Market
        personnelMarket.writeToXml(pw1, 1);

        // Against the Bot
        if (getCampaignOptions().getUseAtB()) {
            contractMarket.writeToXml(pw1, 1);
            unitMarket.writeToXml(pw1, 1);
            MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "colorIndex", colorIndex);
            if (lances.size() > 0)   {
                pw1.println("\t<lances>");
                for (Lance l : lances.values()) {
                    if (forceIds.containsKey(l.getForceId())) {
                        l.writeToXml(pw1, 2);
                    }
                }
                pw1.println("\t</lances>");
            }
            retirementDefectionTracker.writeToXml(pw1, 1);
            if (shipSearchStart != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "shipSearchStart",
                        shortDateFormat.format(shipSearchStart.getTime()));
            }
            MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "shipSearchType", shipSearchType);
            MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "shipSearchResult", shipSearchResult);
            if (shipSearchExpiration != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "shipSearchExpiration",
                        shortDateFormat.format(shipSearchExpiration.getTime()));
            }
        }

        // Customised planetary events
        pw1.println("\t<customPlanetaryEvents>");
        for(Planet p : Planets.getInstance().getPlanets().values()) {
            List<Planet.PlanetaryEvent> customEvents = new ArrayList<>();
            for(Planet.PlanetaryEvent event : p.getEvents()) {
                if(event.custom) {
                    customEvents.add(event);
                }
            }
            if(!customEvents.isEmpty()) {
                pw1.println("\t\t<planet><id>" + p.getId() + "</id>");
                for(Planet.PlanetaryEvent event : customEvents) {
                    Planets.getInstance().writePlanetaryEvent(pw1, event);
                    pw1.println();
                }
                pw1.println("\t\t</planet>");
            }
        }
        pw1.println("\t</customPlanetaryEvents>");

        writeCustoms(pw1);
        // Okay, we're done.
        // Close everything out and be done with it.
        pw1.println("</campaign>");
    }

    public void writeGameOptions(PrintWriter pw1) {
        pw1.println("\t<gameOptions>");
        for (IBasicOption option : getGameOptionsVector()) {
            pw1.println("\t\t<gameoption>"); //$NON-NLS-1$
            MekHqXmlUtil.writeSimpleXmlTag(pw1, 3, "name", option.getName());
            MekHqXmlUtil.writeSimpleXmlTag(pw1, 3, "value", option.getValue()
                    .toString());
            pw1.println("\t\t</gameoption>"); //$NON-NLS-1$
        }
        pw1.println("\t</gameOptions>");
    }

    /**
     * A helper function to encapsulate writing the array/hash pairs out to XML. Each of the types requires a different XML
     * structure, but is in an identical holding structure. Thus, genericized function and interface to cleanly wrap it up.
     * God, I love 3rd-generation programming languages.
     *
     * @param <arrType> The object type in the list. Must implement MekHqXmlSerializable.
     * @param pw1       The PrintWriter to output XML to.
     * @param indent    The indentation level to use for writing XML (purely for neatness).
     * @param tag       The name of the tag to use to encapsulate it.
     * @param array     The list of objects to write out.
     * @param hashtab   The lookup hashtable for the associated array.
     */
    private <arrType> void writeArrayAndHashToXml(PrintWriter pw1, int indent,
            String tag, ArrayList<arrType> array, Hashtable<Integer, arrType> hashtab) {
        // Hooray for implicitly-type-detected genericized functions!
        // However, I still ended up making an interface to handle this.
        // That way, I can cast it and call "writeToXml" to make it cleaner.
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<" + tag + ">");

        // Enumeration<Integer> = hashtab.keys
        for (Integer x : hashtab.keySet()) {
            ((MekHqXmlSerializable) (hashtab.get(x))).writeToXml(pw1,
                    indent + 1);
        }

        pw1.println(MekHqXmlUtil.indentStr(indent) + "</" + tag + ">");
    }

    /**
     * A helper function to encapsulate writing the array/hash pairs out to XML. Each of the types requires a different XML
     * structure, but is in an identical holding structure. Thus, genericized function and interface to cleanly wrap it up.
     * God, I love 3rd-generation programming languages.
     *
     * @param <arrType> The object type in the list. Must implement MekHqXmlSerializable.
     * @param pw1       The PrintWriter to output XML to.
     * @param indent    The indentation level to use for writing XML (purely for neatness).
     * @param tag       The name of the tag to use to encapsulate it.
     * @param array     The list of objects to write out.
     * @param hashtab   The lookup hashtable for the associated array.
     */
    private <arrType> void writeArrayAndHashToXmlforUUID(PrintWriter pw1,
            int indent, String tag, ArrayList<arrType> array, Hashtable<UUID, arrType> hashtab) {
        // Hooray for implicitly-type-detected genericized functions!
        // However, I still ended up making an interface to handle this.
        // That way, I can cast it and call "writeToXml" to make it cleaner.
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<" + tag + ">");

        // Enumeration<Integer> = hashtab.keys
        for (UUID x : hashtab.keySet()) {
            ((MekHqXmlSerializable) (hashtab.get(x))).writeToXml(pw1,
                    indent + 1);
        }

        pw1.println(MekHqXmlUtil.indentStr(indent) + "</" + tag + ">");
    }
    
    /**
     * A helper function to encapsulate writing the map entries out to XML.
     *
     * @param <keyType> The key type of the map.
     * @param <arrType> The object type of the map. Must implement MekHqXmlSerializable.
     * @param pw1       The PrintWriter to output XML to.
     * @param indent    The indentation level to use for writing XML (purely for neatness).
     * @param tag       The name of the tag to use to encapsulate it.
     * @param map       The map of objects to write out.
     */
    private <keyType, valueType extends MekHqXmlSerializable> void writeMapToXml(PrintWriter pw1,
            int indent, String tag, Map<keyType, valueType> map) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<" + tag + ">");

        for (Map.Entry<keyType, valueType> x : map.entrySet()) {
            x.getValue().writeToXml(pw1, indent + 1);
        }

        pw1.println(MekHqXmlUtil.indentStr(indent) + "</" + tag + ">");
    }

    private void writeCustoms(PrintWriter pw1) {
        for (String name : customs) {
            MechSummary ms = MechSummaryCache.getInstance().getMech(name);
            if (ms == null) {
                continue;
            }

            MechFileParser mechFileParser = null;
            try {
                mechFileParser = new MechFileParser(ms.getSourceFile());
            } catch (EntityLoadingException ex) {
                MekHQ.getLogger().error(Campaign.class, "writeCustoms(PrintWriter)", ex);
            }
            if (mechFileParser == null) {
                continue;
            }
            Entity en = mechFileParser.getEntity();
            pw1.println("\t<custom>");
            pw1.println("\t\t<name>" + name + "</name>");
            if (en instanceof Mech) {
                pw1.print("\t\t<mtf>");
                pw1.print(((Mech) en).getMtf());
                pw1.print("\t\t</mtf>\n");
            } else {
                pw1.print("\t\t<blk><![CDATA[");

                BuildingBlock blk = BLKFile.getBlock(en);
                for (String s : blk.getAllDataAsString()) {
                    if (s.isEmpty()) {
                        continue;
                    }
                    pw1.print(s + "\n");
                }
                pw1.print("]]>\n\t\t</blk>\n");
            }
            pw1.println("\t</custom>");
        }
    }

    /**
     * Designed to create a campaign object from a file containing an XML structure. Instead of actually manually parsing
     * it all, lets pull it into a DOM and parse that.
     *
     * @param fis The file holding the XML, in FileInputStream form.
     * @return The created Campaign object, or null if there was a problem.
     * @throws ParseException
     * @throws DOMException
     */
    public static Campaign createCampaignFromXMLFileInputStream(
            FileInputStream fis, MekHQ app) throws DOMException, ParseException,
            NullEntityException {
        final String METHOD_NAME = "createCampaignFromXMLFileInputStream(FileInputStream,MekHQ)"; //$NON-NLS-1$

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Starting load of campaign file from XML..."); //$NON-NLS-1$
        // Initialize variables.
        Campaign retVal = new Campaign();
        retVal.app = app;

        Document xmlDoc = null;

        try {
            // Using factory get an instance of document builder
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            MekHQ.getLogger().error(Campaign.class, METHOD_NAME, ex);
        }

        Element campaignEle = xmlDoc.getDocumentElement();
        NodeList nl = campaignEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML. At least this cleans it up.
        campaignEle.normalize();

        Version version = new Version(campaignEle.getAttribute("version"));

        // we need to iterate through three times, the first time to collect
        // any custom units that might not be written yet
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (wn.getParentNode() != campaignEle) {
                continue;
            }

            int xc = wn.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this
                // level.
                // Okay, so what element is it?
                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("info")) { // This is needed so that the campaign name gets set in retVal
                    processInfoNode(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("custom")) {
                    processCustom(retVal, wn);
                }
            } else {
                // If it's a text node or attribute or whatever at this level,
                // it's probably white-space.
                // We can safely ignore it even if it isn't, for now.
                continue;
            }
        }
        MechSummaryCache.getInstance().loadMechData();

        // the second time to check for any null entities
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (wn.getParentNode() != campaignEle) {
                continue;
            }

            int xc = wn.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this
                // level.
                // Okay, so what element is it?
                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("units")) {
                    String missingList = checkUnits(wn);
                    if (null != missingList) {
                        throw new NullEntityException(missingList);
                    }
                }
            } else {
                // If it's a text node or attribute or whatever at this level,
                // it's probably white-space.
                // We can safely ignore it even if it isn't, for now.
                continue;
            }
        }

        boolean foundPersonnelMarket = false;
        boolean foundContractMarket = false;
        boolean foundUnitMarket = false;

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (wn.getParentNode() != campaignEle) {
                continue;
            }

            int xc = wn.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this
                // level.
                // Okay, so what element is it?
                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("campaignOptions")) {
                    retVal.campaignOptions = CampaignOptions
                            .generateCampaignOptionsFromXml(wn);
                } else if (xn.equalsIgnoreCase("randomSkillPreferences")) {
                    retVal.rskillPrefs = RandomSkillPreferences
                            .generateRandomSkillPreferencesFromXml(wn);
                } /* We don't need this since info is processed above in the first iteration...
                else if (xn.equalsIgnoreCase("info")) {
                    processInfoNode(retVal, wn, version);
                }*/ else if (xn.equalsIgnoreCase("parts")) {
                    processPartNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("personnel")) {
                    // TODO: Make this depending on campaign options
                    InjuryTypes.registerAll();
                    processPersonnelNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("ancestors")) {
                    processAncestorNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("units")) {
                    processUnitNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("missions")) {
                    processMissionNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("forces")) {
                    processForces(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("finances")) {
                    processFinances(retVal, wn);
                } else if (xn.equalsIgnoreCase("location")) {
                    retVal.location = CurrentLocation.generateInstanceFromXML(
                            wn, retVal);
                } else if (xn.equalsIgnoreCase("skillTypes")) {
                    processSkillTypeNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("specialAbilities")) {
                    processSpecialAbilityNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("gameOptions")) {
                    processGameOptionNodes(retVal, wn);
                } else if (xn.equalsIgnoreCase("kills")) {
                    processKillNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("shoppingList")) {
                    retVal.shoppingList = ShoppingList.generateInstanceFromXML(
                            wn, retVal, version);
                } else if (xn.equalsIgnoreCase("personnelMarket")) {
                    retVal.personnelMarket = PersonnelMarket.generateInstanceFromXML(
                            wn, retVal, version);
                    foundPersonnelMarket = true;
                } else if (xn.equalsIgnoreCase("contractMarket")) {
                    retVal.contractMarket = ContractMarket.generateInstanceFromXML(
                            wn, retVal, version);
                    foundContractMarket = true;
                } else if (xn.equalsIgnoreCase("unitMarket")) {
                    retVal.unitMarket = UnitMarket.generateInstanceFromXML(
                            wn, retVal, version);
                    foundUnitMarket = true;
                } else if (xn.equalsIgnoreCase("lances")) {
                    processLanceNodes(retVal, wn);
                } else if (xn.equalsIgnoreCase("retirementDefectionTracker")) {
                    retVal.retirementDefectionTracker = RetirementDefectionTracker.generateInstanceFromXML(wn, retVal);
                } else if (xn.equalsIgnoreCase("shipSearchStart")) {
                    retVal.shipSearchStart = new GregorianCalendar();
                    retVal.shipSearchStart.setTime(retVal.shortDateFormat.parse(wn.getTextContent()));
                } else if (xn.equalsIgnoreCase("shipSearchType")) {
                    retVal.shipSearchType = Integer.parseInt(wn.getTextContent());
                } else if (xn.equalsIgnoreCase("shipSearchResult")) {
                    retVal.shipSearchResult = wn.getTextContent();
                } else if (xn.equalsIgnoreCase("shipSearchExpiration")) {
                    retVal.shipSearchExpiration = new GregorianCalendar();
                    retVal.shipSearchExpiration.setTime(retVal.shortDateFormat.parse(wn.getTextContent()));
                } else if (xn.equalsIgnoreCase("customPlanetaryEvents")) {
                    updatePlanetaryEventsFromXML(wn);
                }

            } else {
                // If it's a text node or attribute or whatever at this level,
                // it's probably white-space.
                // We can safely ignore it even if it isn't, for now.
                continue;
            }
        }

        // Okay, after we've gone through all the nodes and constructed the
        // Campaign object...
        // We need to do a post-process pass to restore a number of references.

        // If the version is earlier than 0.3.4 r1782, then we need to translate
        // the rank system.
        if (Version.versionCompare(version, "0.3.4-r1782")) {
            retVal.setRankSystem(
                    RankTranslator.translateRankSystem(retVal.getRanks().getOldRankSystem(), retVal.getFactionCode()));
            if (retVal.getRanks() == null) {

            }
        }

        // if the version is earlier than 0.1.14, then we need to replace all
        // the old integer
        // ids of units and personnel with their UUIDs where they are
        // referenced.
        if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2
                && version.getSnapshot() < 14) {
            fixIdReferences(retVal);
        }
        // if the version is earlier than 0.1.16, then we need to run another
        // fix to update
        // the externalIds to match the Unit IDs.
        if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2
                && version.getSnapshot() < 16) {
            fixIdReferencesB(retVal);
        }

        // adjust tech levels for version before 0.1.21
        if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2
                && version.getSnapshot() < 21) {
            retVal.campaignOptions.setTechLevel(retVal.campaignOptions
                    .getTechLevel() + 1);
        }

        long timestamp = System.currentTimeMillis();

        // loop through forces to set force id
        for (Map.Entry<Integer, Force> mf : retVal.forceIds.entrySet()) {
            int fid = mf.getKey();
            Force f = mf.getValue();
            Scenario s = retVal.getScenario(f.getScenarioId());
            if (null != s
                    && (null == f.getParentForce() || !f.getParentForce().isDeployed())) {
                s.addForces(fid);
            }
            // some units may need force id set for backwards compatability
            // some may also need scenario id set
            for (UUID uid : f.getUnits()) {
                Unit u = retVal.getUnit(uid);
                if (null != u) {
                    u.setForceId(f.getId());
                    if (f.isDeployed()) {
                        u.setScenarioId(f.getScenarioId());
                    }
                }
            }
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Force IDs set in %dms", System.currentTimeMillis() - timestamp)); //$NON-NLS-1$
        timestamp = System.currentTimeMillis();

        // Process parts...
        ArrayList<Part> spareParts = new ArrayList<Part>();
        ArrayList<Part> removeParts = new ArrayList<Part>();
        for (Part prt : retVal.getParts()) {
            Unit u = retVal.getUnit(prt.getUnitId());
            prt.setUnit(u);
            if (null != u) {
                // get rid of any equipmentparts without locations or mounteds
                if (prt instanceof EquipmentPart) {
                    Mounted m = u.getEntity().getEquipment(
                            ((EquipmentPart) prt).getEquipmentNum());
                    if (null == m || m.getLocation() == Entity.LOC_NONE) {
                        removeParts.add(prt);
                        continue;
                    }
                    // Remove existing duplicate parts.
                    if (u.getPartForEquipmentNum(((EquipmentPart) prt).getEquipmentNum(), ((EquipmentPart) prt).getLocation()) != null) {
                        removeParts.add(prt);
                        continue;
                    }
                }
                if (prt instanceof MissingEquipmentPart) {
                    Mounted m = u.getEntity().getEquipment(
                            ((MissingEquipmentPart) prt).getEquipmentNum());
                    if (null == m || m.getLocation() == Entity.LOC_NONE) {
                        removeParts.add(prt);
                        continue;
                    }
                    // Remove existing duplicate parts.
                    if (u.getPartForEquipmentNum(((MissingEquipmentPart) prt).getEquipmentNum(), ((MissingEquipmentPart) prt).getLocation()) != null) {
                        removeParts.add(prt);
                        continue;
                    }
                }
                //if the type is a BayWeapon, remove
                if(prt instanceof EquipmentPart
                        && ((EquipmentPart) prt).getType() instanceof BayWeapon) {
                    removeParts.add(prt);
                    continue;
                }

                if(prt instanceof MissingEquipmentPart
                        && ((MissingEquipmentPart) prt).getType() instanceof BayWeapon) {
                    removeParts.add(prt);
                    continue;
                }

                // if actuators on units have no location (on version 1.23 and
                // earlier) then remove them and let initializeParts (called
                // later) create new ones
                if (prt instanceof MekActuator
                        && ((MekActuator) prt).getLocation() == Entity.LOC_NONE) {
                    removeParts.add(prt);
                } else if (prt instanceof MissingMekActuator
                        && ((MissingMekActuator) prt).getLocation() == Entity.LOC_NONE) {
                    removeParts.add(prt);
                } else if ((u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) && (prt instanceof EnginePart || prt instanceof MissingEnginePart)) {
                    //units from earlier versions might have the wrong kind of engine
                    removeParts.add(prt);
                } else {
                    u.addPart(prt);
                    if (prt instanceof AmmoBin) {
                        ((AmmoBin) prt).restoreMunitionType();
                    }
                }

            } else if (version.getMajorVersion() == 0
                    && version.getMinorVersion() < 2 && version.getSnapshot() < 16) {
                boolean found = false;
                for (Part spare : spareParts) {
                    if (spare.isSamePartTypeAndStatus(prt)) {
                        spare.incrementQuantity();
                        removeParts.add(prt);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    spareParts.add(prt);
                }
            }
            if (prt instanceof MissingPart) {
                // Missing Parts should only exist on units, but there have
                // been cases where they continue to float around outside of units
                // so this should clean that up
                if (null == u) {
                    removeParts.add(prt);
                } else {
                    // run this to make sure that slots for missing parts are set as
                    // unrepairable
                    // because they will not be in missing locations
                    prt.updateConditionFromPart();
                }
            }
            // old versions didnt distinguish tank engines
            if (prt instanceof EnginePart && prt.getName().contains("Vehicle")) {
                boolean isHover = null != u
                        && u.getEntity().getMovementMode() == EntityMovementMode.HOVER && u.getEntity() instanceof Tank;
                ((EnginePart) prt).fixTankFlag(isHover);
            }
            // clan flag might not have been properly set in early versions
            if (prt instanceof EnginePart && prt.getName().contains("(Clan")
                    && prt.getTechBase() != Part.T_CLAN) {
                ((EnginePart) prt).fixClanFlag();
            }
            if (prt instanceof MissingEnginePart && null != u
                    && u.getEntity() instanceof Tank) {
                boolean isHover = null != u
                        && u.getEntity().getMovementMode() == EntityMovementMode.HOVER && u.getEntity() instanceof Tank;
                ((MissingEnginePart) prt).fixTankFlag(isHover);
            }
            if (prt instanceof MissingEnginePart
                    && prt.getName().contains("(Clan") && prt.getTechBase() != Part.T_CLAN) {
                ((MissingEnginePart) prt).fixClanFlag();
            }

            if ((version.getMajorVersion() == 0)
                    && ((version.getMinorVersion() < 44)
                            || ((version.getMinorVersion() == 43) && (version.getSnapshot() < 7)))) {
                if ((prt instanceof MekLocation)
                        && (((MekLocation)prt).getStructureType() == EquipmentType.T_STRUCTURE_ENDO_STEEL)) {
                    if (null != u) {
                        ((MekLocation)prt).setClan(TechConstants.isClan(u.getEntity().getStructureTechLevel()));
                    } else {
                        ((MekLocation)prt).setClan(retVal.getFaction().isClan());
                    }
                } else if ((prt instanceof MissingMekLocation)
                        && (((MissingMekLocation)prt).getStructureType() == EquipmentType.T_STRUCTURE_ENDO_STEEL)) {
                    if (null != u) {
                        ((MissingMekLocation)prt).setClan(TechConstants.isClan(u.getEntity().getStructureTechLevel()));
                    }
                }
            }

        }
        for (Part prt : removeParts) {
            retVal.removePart(prt);
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Parts processed in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        // All personnel need the rank reference fixed
        for (Person psn : retVal.getPersonnel()) {
            // skill types might need resetting
            psn.resetSkillTypes();

            //versions before 0.3.4 did not have proper clan phenotypes
            if (version.getMajorVersion() == 0
                    && (version.getMinorVersion() <= 2 ||
                            (version.getMinorVersion() <= 3 && version.getSnapshot() < 4))
                    && retVal.getFaction().isClan()) {
                //assume personnel are clan and trueborn if the right role
                psn.setClanner(true);
                switch (psn.getPrimaryRole()) {
                    case Person.T_MECHWARRIOR:
                        psn.setPhenotype(Person.PHENOTYPE_MW);
                        break;
                    case Person.T_AERO_PILOT:
                    case Person.T_CONV_PILOT:
                        psn.setPhenotype(Person.PHENOTYPE_AERO);
                    case Person.T_BA:
                        psn.setPhenotype(Person.PHENOTYPE_BA);
                    case Person.T_VEE_GUNNER:
                    case Person.T_GVEE_DRIVER:
                    case Person.T_NVEE_DRIVER:
                    case Person.T_VTOL_PILOT:
                        psn.setPhenotype(Person.PHENOTYPE_VEE);
                    default:
                        psn.setPhenotype(Person.PHENOTYPE_NONE);
                }
            }
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Rank references fixed in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        // Okay, Units, need their pilot references fixed.
        for(Unit unit : retVal.getUnits()) {
            // Also, the unit should have its campaign set.
            unit.campaign = retVal;

            // reset the pilot and entity, to reflect newly assigned personnel
            unit.resetPilotAndEntity();

            if (null != unit.getRefit()) {
                unit.getRefit().reCalc();
                if (null == unit.getRefit().getNewArmorSupplies()
                        && unit.getRefit().getNewArmorSuppliesId() > 0) {
                    Armor armorSupplies = (Armor) retVal.getPart(
                            unit.getRefit().getNewArmorSuppliesId());
                    unit.getRefit().setNewArmorSupplies(armorSupplies);
                    if (null == armorSupplies.getUnit()) {
                        armorSupplies.setUnit(unit);
                    }
                }
                if (!unit.getRefit().isCustomJob()
                        && !unit.getRefit().kitFound()) {
                    retVal.shoppingList.addShoppingItemWithoutChecking(unit
                            .getRefit());
                }
            }

            // lets make sure the force id set actually corresponds to a force
            // TODO: we have some reports of force id relics - need to fix
            if (unit.getForceId() > 0
                    && null == retVal.getForce(unit.getForceId())) {
                unit.setForceId(-1);
            }

            // Its annoying to have to do this, but this helps to ensure
            // that equipment numbers correspond to the right parts - its
            // possible that these might have changed if changes were made to
            // the
            // ordering of equipment in the underlying data file for the unit
            Utilities.unscrambleEquipmentNumbers(unit);

            // some units might need to be assigned to scenarios
            Scenario s = retVal.getScenario(unit.getScenarioId());
            if (null != s) {
                // most units will be properly assigned through their
                // force, so check to make sure they aren't already here
                if (!s.isAssigned(unit, retVal)) {
                    s.addUnit(unit.getId());
                }
            }

            //get rid of BA parts before 0.3.4
            if(unit.getEntity() instanceof BattleArmor
                    && version.getMajorVersion() == 0
                    && (version.getMinorVersion() <= 2 ||
                            (version.getMinorVersion() <= 3 && version.getSnapshot() < 16))) {
                for(Part p : unit.getParts()) {
                    retVal.removePart(p);
                }
                unit.resetParts();
                if(version.getSnapshot() < 4) {
                    for (int loc = 0; loc < unit.getEntity().locations(); loc++) {
                        unit.getEntity().setInternal(0, loc);
                    }
                }
            }
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Pilot references fixed in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        for(Unit unit : retVal.getUnits()) {
            // Some units have been incorrectly assigned a null C3UUID as a string. This should correct that by setting a new C3UUID
            if ((unit.getEntity().hasC3() || unit.getEntity().hasC3i())
                    && (unit.getEntity().getC3UUIDAsString() == null || unit.getEntity().getC3UUIDAsString().equals("null"))) {
                unit.getEntity().setC3UUID();
                unit.getEntity().setC3NetIdSelf();
            }
        }
        retVal.refreshNetworks();

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] C3 networks refreshed in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        // ok, once we are sure that campaign has been set for all units, we can
        // now go through and initializeParts and run diagnostics
        List<Unit> removeUnits = new ArrayList<>();
        for (Unit unit : retVal.getUnits()) {
            // just in case parts are missing (i.e. because they weren't tracked
            // in previous versions)            
            unit.initializeParts(true);
            unit.runDiagnostic(false);
            if (!unit.isRepairable()) {
                if (!unit.hasSalvageableParts()) {
                    // we shouldnt get here but some units seem to stick around
                    // for some reason
                    removeUnits.add(unit);
                } else {
                    unit.setSalvage(true);
                }
            }
        }
        for (Unit unit : removeUnits) {
            retVal.removeUnit(unit.getId());
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Units initialized in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        retVal.reloadNews();

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] News loaded in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        // If we don't have a personnel market, create one.
        if (!foundPersonnelMarket) {
            retVal.personnelMarket = new PersonnelMarket(retVal);
        }
        if (!foundContractMarket) {
            retVal.contractMarket = new ContractMarket();
        }
        if (!foundUnitMarket) {
            retVal.unitMarket = new UnitMarket();
        }
        if (null == retVal.retirementDefectionTracker) {
            retVal.retirementDefectionTracker = new RetirementDefectionTracker();
        }
        if (retVal.getCampaignOptions().getUseAtB()) {
            retVal.atbConfig = AtBConfiguration.loadFromXml();
            retVal.atbEventProcessor = new AtBEventProcessor(retVal);
        }

        //**EVERYTHING HAS BEEN LOADED. NOW FOR SANITY CHECKS**//

        //unload any ammo bins in the warehouse
        ArrayList<AmmoBin> binsToUnload = new ArrayList<AmmoBin>();
        for(Part prt : retVal.getSpareParts()) {
            if (prt instanceof AmmoBin && !prt.isReservedForRefit() && ((AmmoBin) prt).getShotsNeeded() == 0) {
                binsToUnload.add((AmmoBin) prt);
            }
        }
        for(AmmoBin bin : binsToUnload) {
            bin.unload();
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Ammo bins cleared in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();


        //Check all parts that are reserved for refit and if the refit id unit
        //is not refitting or is gone then unreserve
        for(Part part : retVal.getParts()) {
            if (part.isReservedForRefit()) {
                Unit u = retVal.getUnit(part.getRefitId());
                if (null == u || !u.isRefitting()) {
                    part.setRefitId(null);
                }
            }
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Reserved refit parts fixed in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        //try to stack as much as possible the parts in the warehouse that may be unstacked
        //for a variety of reasons
        ArrayList<Part> partsToRemove = new ArrayList<Part>();
        ArrayList<Part> partsToKeep = new ArrayList<Part>();
        for(Part part : retVal.getParts()) {
            if (part.isSpare() && part.isPresent()) {
                for (Part oPart : partsToKeep) {
                    if (part.isSamePartTypeAndStatus(oPart)) {
                        if (part instanceof Armor) {
                            if (oPart instanceof Armor) {
                                ((Armor) oPart).setAmount(((Armor) oPart).getAmount() + ((Armor) part).getAmount());
                                partsToRemove.add(part);
                                break;
                            }
                        } else if (part instanceof AmmoStorage) {
                            if (oPart instanceof AmmoStorage) {
                                ((AmmoStorage) oPart).changeShots(((AmmoStorage) part).getShots());
                                partsToRemove.add(part);
                                break;
                            }
                        } else {
                            int q = part.getQuantity();
                            while (q > 0) {
                                oPart.incrementQuantity();
                                q--;
                            }
                            partsToRemove.add(part);
                            break;
                        }
                    }
                }
                partsToKeep.add(part);
            }
        }
        for(Part toRemove : partsToRemove) {
            retVal.removePart(toRemove);
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Warehouse cleaned up in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        retVal.unitRating = null;

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Load of campaign file complete!"); //$NON-NLS-1$

        return retVal;
    }

    private static void updatePlanetaryEventsFromXML(Node wn) {
        Planets.reload(true);
        NodeList wList = wn.getChildNodes();
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (wn2.getNodeName().equalsIgnoreCase("planet")) {
                NodeList planetNodes = wn2.getChildNodes();
                String planetId = null;
                List<Planet.PlanetaryEvent> events = new ArrayList<>();
                for(int n = 0; n < planetNodes.getLength(); ++ n) {
                    Node planetNode = planetNodes.item(n);
                    if(planetNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if(planetNode.getNodeName().equalsIgnoreCase("id")) {
                        planetId = planetNode.getTextContent();
                    } else if(planetNode.getNodeName().equalsIgnoreCase("event")) {
                        Planet.PlanetaryEvent event = Planets.getInstance().readPlanetaryEvent(planetNode);
                        if(null != event) {
                            event.custom = true;
                            events.add(event);
                        }
                    }
                }
                if(null != planetId) {
                    Planets.getInstance().updatePlanetaryEvents(planetId, events, true);
                }
            }
        }
    }

    private static void fixIdReferences(Campaign retVal) {
        // set up translation hashes
        Map<Integer, UUID> uHash = new HashMap<>();
        Map<Integer, UUID> pHash = new HashMap<>();
        for (Unit u : retVal.getUnits()) {
            uHash.put(u.getOldId(), u.getId());
        }
        for (Person p : retVal.getPersonnel()) {
            pHash.put(p.getOldId(), p.getId());
        }
        // ok now go through and fix
        for (Unit u : retVal.getUnits()) {
            u.fixIdReferences(uHash, pHash);
        }
        for (Person p : retVal.getPersonnel()) {
            p.fixIdReferences(uHash, pHash);
        }
        retVal.forces.fixIdReferences(uHash);
        for (Part p : retVal.getParts()) {
            p.fixIdReferences(uHash, pHash);
        }
        ArrayList<Kill> ghostKills = new ArrayList<Kill>();
        for (Kill k : retVal.kills) {
            k.fixIdReferences(pHash);
            if (null == k.getPilotId()) {
                ghostKills.add(k);
            }
        }
        // check for kills with missing person references
        for (Kill k : ghostKills) {
            if (null == k.getPilotId()) {
                retVal.removeKill(k);
            }
        }
    }

    private static void fixIdReferencesB(Campaign retVal) {
        for (Map.Entry<UUID, Unit> u : retVal.units.entrySet()) {
            Entity en = u.getValue().getEntity();
            UUID id = u.getKey();
            en.setExternalIdAsString(id.toString());

            // If they have C3 or C3i we need to set their ID
            if (en.hasC3() || en.hasC3i()) {
                en.setC3UUID();
                en.setC3NetIdSelf();
            }
        }
    }

    private static void processFinances(Campaign retVal, Node wn) {
        MekHQ.getLogger().log(Campaign.class, "processFinances(Campaign,Node)", LogLevel.INFO, //$NON-NLS-1$
                "Loading Finances from XML..."); //$NON-NLS-1$
        retVal.finances = Finances.generateInstanceFromXML(wn);
        MekHQ.getLogger().log(Campaign.class, "processFinances(Campaign,Node)", LogLevel.INFO, //$NON-NLS-1$
                "Load of Finances complete!"); //$NON-NLS-1$
    }

    private static void processForces(Campaign retVal, Node wn, Version version) {
        final String METHOD_NAME = "processForces(Campaign,Node,Version)"; //$NON-NLS-1$
        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Loading Force Organization from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        boolean foundForceAlready = false;
        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("force")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Forces nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            if (!foundForceAlready) {
                Force f = Force.generateInstanceFromXML(wn2, retVal, version);
                if (null != f) {
                    retVal.forces = f;
                    foundForceAlready = true;
                }
            } else {
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.ERROR,
                        "More than one type-level force found"); //$NON-NLS-1$
            }
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Load of Force Organization complete!");
    }

    private static void processPersonnelNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processPersonnelNodes(Campaign,Node,Version)"; //$NON-NLS-1$
        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Loading Personnel Nodes from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("person")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Personnel nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Person p = Person.generateInstanceFromXML(wn2, retVal, version);

            if (p != null) {
                retVal.addPersonWithoutId(p);
            }
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Load Personnel Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processAncestorNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processAncestorNodes(Campaign,Node,Version)"; //$NON-NLS-1$

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Loading Ancestor Nodes from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("ancestor")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Ancestor nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Ancestors a = Ancestors.generateInstanceFromXML(wn2, retVal, version);

            if (a != null) {
                retVal.addAncestorsWithoutId(a);
            }
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Load Ancestor Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processSkillTypeNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processSkillTypeNodes(Campaign,Node,Version)"; //$NON-NLS-1$

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Loading Skill Type Nodes from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (wn2.getNodeName().startsWith("ability-")) {
                continue;
            } else if (!wn2.getNodeName().equalsIgnoreCase("skillType")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Skill Type nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            SkillType.generateInstanceFromXML(wn2, version);
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Load Skill Type Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processSpecialAbilityNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processSpecialAbilityNodes(Campaign,Node,Version)"; //$NON-NLS-1$

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Loading Special Ability Nodes from XML..."); //$NON-NLS-1$

        PilotOptions options = new PilotOptions();
        SpecialAbility.clearSPA();

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("ability")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Special Ability nodes: " //$NON-NLS-1$
                        + wn2.getNodeName());

                continue;
            }

            SpecialAbility.generateInstanceFromXML(wn2, options, version);
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Load Special Ability Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processKillNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processKillNodes(Campaign,Node,Version"; //$NON-NLS-1$
        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Loading Kill Nodes from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            } else if (!wn2.getNodeName().equalsIgnoreCase("kill")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Kill nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            retVal.kills.add(Kill.generateInstanceFromXML(wn2, version));
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Load Kill Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processGameOptionNodes(Campaign retVal, Node wn) {
        final String METHOD_NAME = "processGameOptionNodes(Campaign,Node)"; //$NON-NLS-1$
        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Loading GameOption Nodes from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            } else if (!wn2.getNodeName().equalsIgnoreCase("gameoption")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Game Option nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }
            NodeList nl = wn2.getChildNodes();

            String name = null;
            Object value = null;
            for (int y = 0; y < nl.getLength(); y++) {
                Node wn3 = nl.item(y);
                if (wn3.getNodeName().equalsIgnoreCase("name")) {
                    name = wn3.getTextContent();
                } else if (wn3.getNodeName().equalsIgnoreCase("value")) {
                    value = wn3.getTextContent();
                }
            }
            if ((null != name) && (null != value)) {
                IOption option = retVal.gameOptions.getOption(name);
                if (null != option) {
                    if (!option.getValue().toString().equals(value.toString())) {
                        try {
                            switch (option.getType()) {
                                case IOption.STRING:
                                case IOption.CHOICE:
                                    option.setValue((String) value);
                                    break;

                                case IOption.BOOLEAN:
                                    option.setValue(new Boolean(value.toString()));
                                    break;

                                case IOption.INTEGER:
                                    option.setValue(new Integer(value.toString()));
                                    break;

                                case IOption.FLOAT:
                                    option.setValue(new Float(value.toString()));
                                    break;

                            }
                        } catch (IllegalArgumentException iaEx) {
                            MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.ERROR,
                                    "Error trying to load option '" + name + "' with a value of '" //$NON-NLS-1$
                                    + value + "'.");
                        }
                    }
                } else {
                    MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.ERROR,
                            "Invalid option '" + name + "' when trying to load options file."); //$NON-NLS-1$
                }
            }
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Load Game Option Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processCustom(Campaign retVal, Node wn) {
        final String METHOD_NAME = "processCustom(Campaign,Node)"; //$NON-NLS-1$

        String sCustomsDir = "data" + File.separator + "mechfiles"
                + File.separator + "customs";
        String sCustomsDirCampaign = sCustomsDir + File.separator + retVal.getName();
        File customsDir = new File(sCustomsDir);
        if (!customsDir.exists()) {
            customsDir.mkdir();
        }
        File customsDirCampaign = new File(sCustomsDirCampaign);
        if (!customsDirCampaign.exists()) {
            customsDirCampaign.mkdir();
        }

        NodeList wList = wn.getChildNodes();

        String name = null;
        String mtf = null;
        String blk = null;

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (wn2.getNodeName().equalsIgnoreCase("name")) {
                name = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("mtf")) {
                mtf = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("blk")) {
                blk = wn2.getTextContent();
            }
        }
        retVal.addCustom(name);
        if (null != name && null != mtf) {
            try {
                // if this file already exists then don't overwrite it or we
                // will end up with a bunch of copies
                String fileName = sCustomsDir + File.separator + name + ".mtf";
                String fileNameCampaign = sCustomsDirCampaign + File.separator
                        + name + ".mtf";
                if ((new File(fileName)).exists()
                        || (new File(fileNameCampaign)).exists()) {
                    return;
                }
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                        "Loading Custom unit from XML..."); //$NON-NLS-1$
                FileOutputStream out = new FileOutputStream(fileNameCampaign);
                PrintStream p = new PrintStream(out);
                p.println(mtf);
                p.close();
                out.close();
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                        "Loaded Custom unit!"); //$NON-NLS-1$
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (null != name && null != blk) {
            try {
                // if this file already exists then don't overwrite it or we
                // will end up with a bunch of copies
                String fileName = sCustomsDir + File.separator + name + ".blk";
                String fileNameCampaign = sCustomsDirCampaign + File.separator
                        + name + ".blk";
                if ((new File(fileName)).exists()
                        || (new File(fileNameCampaign)).exists()) {
                    return;
                }
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                        "Loading Custom unit from XML..."); //$NON-NLS-1$
                FileOutputStream out = new FileOutputStream(fileNameCampaign);
                PrintStream p = new PrintStream(out);
                p.println(blk);
                p.close();
                out.close();
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                        "Loaded Custom unit!"); //$NON-NLS-1$
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void processMissionNodes(Campaign retVal, Node wn, Version version) {
        final String METHOD_NAME = "processMissionNodes(Campaign,Node,Version)"; //$NON-NLS-1$

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Loading Mission Nodes from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("mission")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                        "Unknown node type not loaded in Mission nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Mission m = Mission.generateInstanceFromXML(wn2, retVal, version);

            if (m != null) {
                // add scenarios to the scenarioId hash
                for (Scenario s : m.getScenarios()) {
                    retVal.addScenarioToHash(s);
                }
                retVal.addMissionWithoutId(m);
            }
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Load Mission Nodes Complete!"); //$NON-NLS-1$
    }

    private static String checkUnits(Node wn) {
        final String METHOD_NAME = "checkUnits(Node)"; //$NON-NLS-1$

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                "Checking for missing entities..."); //$NON-NLS-1$

        ArrayList<String> unitList = new ArrayList<String>();
        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("unit")) {
                continue;
            }

            NodeList nl = wn2.getChildNodes();

            for (int y = 0; y < nl.getLength(); y++) {
                Node wn3 = nl.item(y);
                try {
                    if (wn3.getNodeName().equalsIgnoreCase("entity")) {
                        if (null == MekHqXmlUtil.getEntityFromXmlString(wn3)) {
                            String name = MekHqXmlUtil
                                    .getEntityNameFromXmlString(wn3);
                            if (!unitList.contains(name)) {
                                unitList.add(name);
                            }
                        }
                    }
                } catch (Exception ex) {

                }
            }
        }
        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Finished checking for missing entities!"); //$NON-NLS-1$

        if (unitList.isEmpty()) {
            return null;
        } else {
            String unitListString = "";
            for (String s : unitList) {
                unitListString += "\n" + s;
            }
            return unitListString;
        }
    }

    private static void processUnitNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processUnitNodes(Campaign,Node,Version)"; //$NON-NLS-1$

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Loading Unit Nodes from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("unit")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Unit nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Unit u = Unit.generateInstanceFromXML(wn2, version);

            if (u != null) {
                retVal.addUnit(u);
            }
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Load Unit Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processPartNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processPartNodes(Campaign,Node,Version)"; //$NON-NLS-1$

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Loading Part Nodes from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("part")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Part nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Part p = Part.generateInstanceFromXML(wn2, version);

            // deal with the Weapon as Heat Sink problem from earlier versions
            if (p instanceof HeatSink && !p.getName().contains("Heat Sink")) {
                continue;
            }

            if (((p instanceof EquipmentPart) && ((EquipmentPart) p).getType() == null)
                    || ((p instanceof MissingEquipmentPart) && ((MissingEquipmentPart) p).getType() == null)) {
                MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.WARNING,
                        "Could not find matching EquipmentType for part " + p.getName());
                continue;
            }

            // deal with equipmentparts that are now subtyped
            int pid = p.getId();
            if (p instanceof EquipmentPart
                    && ((EquipmentPart) p).getType() instanceof MiscType
                    && ((EquipmentPart) p).getType().hasFlag(MiscType.F_MASC) && !(p instanceof MASC)) {
                p = new MASC(p.getUnitTonnage(), ((EquipmentPart) p).getType(),
                        ((EquipmentPart) p).getEquipmentNum(), retVal, 0, p.isOmniPodded());
                p.setId(pid);
            }
            if (p instanceof MissingEquipmentPart
                    && ((MissingEquipmentPart) p).getType().hasFlag(MiscType.F_MASC) && !(p instanceof MASC)) {
                p = new MissingMASC(p.getUnitTonnage(),
                        ((MissingEquipmentPart) p).getType(), ((MissingEquipmentPart) p).getEquipmentNum(), retVal,
                        ((MissingEquipmentPart) p).getTonnage(), 0, p.isOmniPodded());
                p.setId(pid);
            }
            // deal with true values for sensor and life support on non-Mech
            // heads
            if (p instanceof MekLocation
                    && ((MekLocation) p).getLoc() != Mech.LOC_HEAD) {
                ((MekLocation) p).setSensors(false);
                ((MekLocation) p).setLifeSupport(false);
            }

            if (version.getMinorVersion() < 3 && !p.needsFixing()
                    && !p.isSalvaging()) {
                // repaired parts were not getting experience properly reset
                p.setSkillMin(SkillType.EXP_GREEN);
            }

            //if for some reason we couldn't find a type for equipment part, then remove it
            if((p instanceof EquipmentPart && null == ((EquipmentPart)p).getType())
                    || (p instanceof MissingEquipmentPart && null == ((MissingEquipmentPart) p).getType())) {
                p = null;
            }

            if ((null != p) && (p.getUnitId() != null)
                    && ((version.getMinorVersion() < 43)
                            || ((version.getMinorVersion() == 43) && (version.getSnapshot() < 5)))
                    && ((p instanceof AmmoBin) || (p instanceof MissingAmmoBin))) {
                Unit u = retVal.getUnit(p.getUnitId());
                if ((null != u) && (u.getEntity().usesWeaponBays())) {
                    Mounted ammo;
                    if (p instanceof EquipmentPart) {
                        ammo = u.getEntity().getEquipment(((EquipmentPart) p).getEquipmentNum());
                    } else {
                        ammo = u.getEntity().getEquipment(((MissingEquipmentPart) p).getEquipmentNum());
                    }
                    if (null != ammo) {
                        if (p instanceof AmmoBin) {
                            p = new LargeCraftAmmoBin(p.getUnitTonnage(),
                                    ((AmmoBin) p).getType(),
                                    ((AmmoBin) p).getEquipmentNum(),
                                    ((AmmoBin) p).getShotsNeeded(),
                                    ammo.getAmmoCapacity(), retVal);
                            ((LargeCraftAmmoBin) p).setBay(u.getEntity().getBayByAmmo(ammo));
                        } else {
                            p = new MissingLargeCraftAmmoBin(p.getUnitTonnage(),
                                    ((MissingAmmoBin) p).getType(),
                                    ((MissingAmmoBin) p).getEquipmentNum(),
                                    ammo.getAmmoCapacity(), retVal);
                            ((MissingLargeCraftAmmoBin) p).setBay(u.getEntity().getBayByAmmo(ammo));
                        }
                    }
                }
            }

            if (p != null) {
                p.setCampaign(retVal);
                retVal.addPartWithoutId(p);
            }
        }

        MekHQ.getLogger().log(Campaign.class, METHOD_NAME, LogLevel.INFO,
                "Load Part Nodes Complete!"); //$NON-NLS-1$
    }

    /**
     * Pulled out purely for encapsulation. Makes the code neater and easier to read.
     *
     * @param retVal The Campaign object that is being populated.
     * @param wni    The XML node we're working from.
     * @throws ParseException
     * @throws DOMException
     */
    private static void processInfoNode(Campaign retVal, Node wni,
            Version version) throws DOMException, ParseException {
        NodeList nl = wni.getChildNodes();

        String rankNames = null;
        int officerCut = 0;
        int rankSystem = -1;

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            int xc = wn.getNodeType();

            // If it's not an element, again, we're ignoring it.
            if (xc == Node.ELEMENT_NODE) {
                String xn = wn.getNodeName();

                // Yeah, long if/then clauses suck.
                // I really couldn't think of a significantly better way to
                // handle it.
                // They're all primitives anyway...
                if (xn.equalsIgnoreCase("calendar")) {
                    SimpleDateFormat df = new SimpleDateFormat(
                            "yyyy-MM-dd hh:mm:ss");
                    retVal.calendar = (GregorianCalendar) GregorianCalendar
                            .getInstance();
                    retVal.calendar.setTime(df
                            .parse(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("camoCategory")) {
                    String val = wn.getTextContent().trim();

                    if (val.equals("null")) {
                        retVal.camoCategory = null;
                    } else {
                        retVal.camoCategory = val;
                    }
                } else if (xn.equalsIgnoreCase("camoFileName")) {
                    String val = wn.getTextContent().trim();

                    if (val.equals("null")) {
                        retVal.camoFileName = null;
                    } else {
                        retVal.camoFileName = val;
                    }
                } else if (xn.equalsIgnoreCase("colorIndex")) {
                    retVal.colorIndex = Integer.parseInt(wn.getTextContent()
                            .trim());
                } else if (xn.equalsIgnoreCase("nameGen")) {
                    // First, get all the child nodes;
                    NodeList nl2 = wn.getChildNodes();
                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);
                        if (wn2.getParentNode() != wn) {
                            continue;
                        }
                        if (wn2.getNodeName().equalsIgnoreCase("faction")) {
                            retVal.getRNG().setChosenFaction(wn2.getTextContent().trim());
                        } else if (wn2.getNodeName().equalsIgnoreCase(
                                "percentFemale")) {
                            retVal.getRNG().setPerentFemale(Integer.parseInt(wn2.getTextContent().trim()));
                        }
                    }
                } else if (xn.equalsIgnoreCase("currentReport")) {
                    // First, get all the child nodes;
                    NodeList nl2 = wn.getChildNodes();

                    // Then, make sure the report is empty. *just* in case.
                    // ...That is, creating a new campaign throws in a date line
                    // for us...
                    // So make sure it's cleared out.
                    retVal.currentReport.clear();

                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);

                        if (wn2.getParentNode() != wn) {
                            continue;
                        }

                        if (wn2.getNodeName().equalsIgnoreCase("reportLine")) {
                            retVal.currentReport.add(wn2.getTextContent());
                        }
                    }
                } else if (xn.equalsIgnoreCase("faction")) {
                    if (version.getMajorVersion() == 0
                            && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
                        retVal.factionCode = Faction.getFactionCode(Integer.parseInt(wn.getTextContent()));
                    } else {
                        retVal.factionCode = wn.getTextContent();
                    }
                    retVal.updateTechFactionCode();
                } else if (xn.equalsIgnoreCase("retainerEmployerCode")) {
                    retVal.retainerEmployerCode = wn.getTextContent();
                } else if (xn.equalsIgnoreCase("officerCut")) {
                    officerCut = Integer.parseInt(wn.getTextContent().trim());
                } else if (xn.equalsIgnoreCase("rankNames")) {
                    rankNames = wn.getTextContent().trim();
                } else if (xn.equalsIgnoreCase("ranks") || xn.equalsIgnoreCase("rankSystem")) {
                    if (Version.versionCompare(version, "0.3.4-r1645")) {
                        rankSystem = Integer.parseInt(wn.getTextContent().trim());
                    } else {
                        retVal.ranks = Ranks.generateInstanceFromXML(wn, version);
                    }
                } else if (xn.equalsIgnoreCase("gmMode")) {
                    retVal.gmMode = Boolean.parseBoolean(wn.getTextContent().trim());
                } else if (xn.equalsIgnoreCase("showOverview")) {
                    retVal.overviewLoadingValue = Boolean.parseBoolean(wn.getTextContent().trim());
                } else if (xn.equalsIgnoreCase("lastPartId")) {
                    retVal.lastPartId = Integer.parseInt(wn.getTextContent()
                            .trim());
                } else if (xn.equalsIgnoreCase("lastForceId")) {
                    retVal.lastForceId = Integer.parseInt(wn.getTextContent()
                            .trim());
                } else if (xn.equalsIgnoreCase("lastTeamId")) {
                    retVal.lastTeamId = Integer.parseInt(wn.getTextContent()
                            .trim());
                } else if (xn.equalsIgnoreCase("lastMissionId")) {
                    retVal.lastMissionId = Integer.parseInt(wn.getTextContent()
                            .trim());
                } else if (xn.equalsIgnoreCase("lastScenarioId")) {
                    retVal.lastScenarioId = Integer.parseInt(wn.getTextContent().trim());
                } else if (xn.equalsIgnoreCase("name")) {
                    String val = wn.getTextContent().trim();

                    if (val.equals("null")) {
                        retVal.name = null;
                    } else {
                        retVal.name = val;
                    }
                } else if (xn.equalsIgnoreCase("overtime")) {
                    retVal.overtime = Boolean.parseBoolean(wn.getTextContent().trim());
                } else if (xn.equalsIgnoreCase("astechPool")) {
                    retVal.astechPool = Integer.parseInt(wn.getTextContent()
                            .trim());
                } else if (xn.equalsIgnoreCase("astechPoolMinutes")) {
                    retVal.astechPoolMinutes = Integer.parseInt(wn.getTextContent().trim());
                } else if (xn.equalsIgnoreCase("astechPoolOvertime")) {
                    retVal.astechPoolOvertime = Integer.parseInt(wn.getTextContent().trim());
                } else if (xn.equalsIgnoreCase("medicPool")) {
                    retVal.medicPool = Integer.parseInt(wn.getTextContent().trim());
                } else if (xn.equalsIgnoreCase("fatigueLevel")) {
                    retVal.fatigueLevel = Integer.parseInt(wn.getTextContent().trim());
                } else if (xn.equalsIgnoreCase("id")) {
                    retVal.id = UUID.fromString(wn.getTextContent().trim());
                }
            }
        }
        if (null != rankNames) {
            //backwards compatibility
            retVal.ranks.setRanksFromList(rankNames, officerCut);
        }
        if (rankSystem != -1) {
            retVal.ranks = new Ranks(rankSystem);
            retVal.ranks.setOldRankSystem(rankSystem);
        }
        retVal.currentReportHTML = Utilities.combineString(retVal.currentReport, REPORT_LINEBREAK);
        // Everything's new
        retVal.newReports = new ArrayList<String>(retVal.currentReport.size() * 2);
        boolean firstReport = true;
        for(String report : retVal.currentReport) {
            if (firstReport) {
                firstReport = false;
            } else {
                retVal.newReports.add(REPORT_LINEBREAK);
            }
            retVal.newReports.add(report);
        }
    }

    private static void processLanceNodes(Campaign retVal, Node wn) {
        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("lance")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(Campaign.class, "processLanceNodes(Campaign,Node)", LogLevel.ERROR, //$NON-NLS-1$
                        "Unknown node type not loaded in Lance nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Lance l = Lance.generateInstanceFromXML(wn2);

            if (l != null) {
                retVal.lances.put(l.getForceId(), l);
            }
        }
    }

    public ArrayList<Planet> getPlanets() {
        ArrayList<Planet> plnts = new ArrayList<Planet>();
        for (String key : Planets.getInstance().getPlanets().keySet()) {
            plnts.add(Planets.getInstance().getPlanets().get(key));
        }
        return plnts;
    }

    public Vector<String> getPlanetNames() {
        Vector<String> plntNames = new Vector<String>();
        for (Planet key : Planets.getInstance().getPlanets().values()) {
            plntNames.add(key.getPrintableName(Utilities.getDateTimeDay(calendar)));
        }
        return plntNames;
    }

    public Planet getPlanetByName(String name) {
        return Planets.getInstance().getPlanetByName(name, Utilities.getDateTimeDay(calendar));
    }

    public Person newPerson(int type) {
        return newPerson(type, getFactionCode());
    }

    public Person newPerson(int type, int secondary) {
        return newPerson(type, secondary, getFactionCode());
    }
    
    public Person newPerson(int type, String factionCode) {
        return newPerson(type, Person.T_NONE, factionCode);
    }

    /**
     * Generate a new pilotPerson of the given type using whatever randomization options have been given in the
     * CampaignOptions
     *
     * @param type The primary role
     * @param secondary A secondary role; used for LAM pilots to generate MW + Aero pilot
     * @return
     */
    public Person newPerson(int type, int secondary, String factionCode) {
        if (type == Person.T_LAM_PILOT) {
            type = Person.T_MECHWARRIOR;
            secondary = Person.T_AERO_PILOT;
        }
        boolean isFemale = getRNG().isFemale();
        Person person = new Person(this, factionCode);
        if (isFemale) {
            person.setGender(Person.G_FEMALE);
        }
        person.setName(getRNG().generate(isFemale));
        int bonus = rskillPrefs.getOverallRecruitBonus() + rskillPrefs.getRecruitBonus(type);
        // LAM pilots get +3 to random experience roll
        if ((type == Person.T_MECHWARRIOR) && (secondary == Person.T_AERO_PILOT)) {
            bonus += 3;
        }
        int expLvl = Utilities.generateExpLevel(bonus);
        person.setPrimaryRole(type);
        person.setSecondaryRole(secondary);
        if (getCampaignOptions().useDylansRandomXp()) {
            person.setXp(Utilities.generateRandomExp());
        }
        person.setDaysToWaitForHealing(getCampaignOptions().getNaturalHealingWaitingPeriod());
        //check for clan phenotypes
        bonus = 0;
        if (person.isClanner()) {
            switch (type) {
                case (Person.T_MECHWARRIOR):
                    if (Utilities.rollProbability(getCampaignOptions().getProbPhenoMW())) {
                        person.setPhenotype(Person.PHENOTYPE_MW);
                        bonus = 1;
                    }
                    break;
                case (Person.T_GVEE_DRIVER):
                case (Person.T_NVEE_DRIVER):
                case (Person.T_VTOL_PILOT):
                case (Person.T_VEE_GUNNER):
                    if (Utilities.rollProbability(getCampaignOptions().getProbPhenoVee())) {
                        person.setPhenotype(Person.PHENOTYPE_VEE);
                        bonus = 1;
                    }
                    break;
                case (Person.T_CONV_PILOT):
                case (Person.T_AERO_PILOT):
                case (Person.T_PROTO_PILOT):
                    if (Utilities.rollProbability(getCampaignOptions().getProbPhenoAero())) {
                        person.setPhenotype(Person.PHENOTYPE_AERO);
                        bonus = 1;
                    }
                    break;
                case (Person.T_BA):
                    if (Utilities.rollProbability(getCampaignOptions().getProbPhenoBA())) {
                        person.setPhenotype(Person.PHENOTYPE_BA);
                        bonus = 1;
                    }
                    break;
                default:
                    break;
            }
        }
        GregorianCalendar birthdate = (GregorianCalendar) getCalendar().clone();
        birthdate.set(Calendar.YEAR, birthdate.get(Calendar.YEAR) - Utilities.getAgeByExpLevel(expLvl, person.isClanner() && person.getPhenotype() != Person.PHENOTYPE_NONE));
        // choose a random day and month
        int nDays = 365;
        if (birthdate.isLeapYear(birthdate.get(Calendar.YEAR))) {
            nDays = 366;
        }
        int randomDay = Compute.randomInt(nDays) + 1;
        birthdate.set(Calendar.DAY_OF_YEAR, randomDay);
        person.setBirthday(birthdate);
        // set default skills
        int mod = 0;
        if ((type == Person.T_MECHWARRIOR) && (secondary == Person.T_AERO_PILOT)) {
            mod = -2;
        }
        generateDefaultSkills(type, person, expLvl, bonus, mod);
        if (secondary != Person.T_NONE) {
            generateDefaultSkills(secondary, person, expLvl, bonus, mod);
        }
        // apply phenotype bonus only to primary skills
        bonus = 0;
        // roll small arms skill
        if (!person.hasSkill(SkillType.S_SMALL_ARMS)) {
            int sarmsLvl = -12;
            if (person.isSupport()) {
                sarmsLvl = Utilities.generateExpLevel(rskillPrefs
                        .getSupportSmallArmsBonus());
            } else {
                sarmsLvl = Utilities.generateExpLevel(rskillPrefs
                        .getCombatSmallArmsBonus());
            }
            if (sarmsLvl > SkillType.EXP_ULTRA_GREEN) {
                person.addSkill(SkillType.S_SMALL_ARMS, sarmsLvl,
                        rskillPrefs.randomizeSkill(), bonus);
            }

        }
        // roll tactics skill
        if (!person.isSupport()) {
            int tacLvl = Utilities.generateExpLevel(rskillPrefs
                    .getTacticsMod(expLvl));
            if (tacLvl > SkillType.EXP_ULTRA_GREEN) {
                person.addSkill(SkillType.S_TACTICS, tacLvl,
                        rskillPrefs.randomizeSkill(), bonus);
            }
        }
        // roll artillery skill
        if (getCampaignOptions().useArtillery()
                && (type == Person.T_MECHWARRIOR || type == Person.T_VEE_GUNNER || type == Person.T_INFANTRY)
                && Utilities.rollProbability(rskillPrefs.getArtilleryProb())) {
            int artyLvl = Utilities.generateExpLevel(rskillPrefs
                    .getArtilleryBonus());
            if (artyLvl > SkillType.EXP_ULTRA_GREEN) {
                person.addSkill(SkillType.S_ARTILLERY, artyLvl,
                        rskillPrefs.randomizeSkill(), bonus);
            }
        }
        // roll random secondary skill
        if (Utilities.rollProbability(rskillPrefs.getSecondSkillProb())) {
            ArrayList<String> possibleSkills = new ArrayList<String>();
            for (String stype : SkillType.skillList) {
                if (!person.hasSkill(stype)) {
                    possibleSkills.add(stype);
                }
            }
            String selSkill = possibleSkills.get(Compute
                    .randomInt(possibleSkills.size()));
            int secondLvl = Utilities.generateExpLevel(rskillPrefs
                    .getSecondSkillBonus());
            person.addSkill(selSkill, secondLvl, rskillPrefs.randomizeSkill(),
                    bonus);
        }
        // TODO: roll special abilities
        if (getCampaignOptions().useAbilities()) {
            int nabil = Utilities.rollSpecialAbilities(rskillPrefs
                    .getSpecialAbilBonus(expLvl));
            while (nabil > 0 && null != rollSPA(type, person)) {
                nabil--;
            }
        }
        if (getCampaignOptions().usePortraitForType(type)) {
            assignRandomPortraitFor(person);
        }
        //check for Bloodname
        if (person.isClanner()) {
            checkBloodnameAdd(person, type, factionCode);
        }

        return person;
    }

    private void generateDefaultSkills(int type, Person person, int expLvl, int bonus, int mod) {
        switch (type) {
            case (Person.T_MECHWARRIOR):
                person.addSkill(SkillType.S_PILOT_MECH, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                person.addSkill(SkillType.S_GUN_MECH, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_GVEE_DRIVER):
                person.addSkill(SkillType.S_PILOT_GVEE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                person.addSkill(SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_NVEE_DRIVER):
                person.addSkill(SkillType.S_PILOT_NVEE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                person.addSkill(SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_VTOL_PILOT):
                person.addSkill(SkillType.S_PILOT_VTOL, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                person.addSkill(SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_VEE_GUNNER):
                person.addSkill(SkillType.S_GUN_VEE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_CONV_PILOT):
                person.addSkill(SkillType.S_PILOT_JET, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                person.addSkill(SkillType.S_GUN_JET, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_AERO_PILOT):
                person.addSkill(SkillType.S_PILOT_AERO, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                person.addSkill(SkillType.S_GUN_AERO, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_PROTO_PILOT):
                person.addSkill(SkillType.S_GUN_PROTO, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_BA):
                person.addSkill(SkillType.S_GUN_BA, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                person.addSkill(SkillType.S_ANTI_MECH, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                person.addSkill(SkillType.S_SMALL_ARMS, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_INFANTRY):
                if (Utilities.rollProbability(rskillPrefs.getAntiMekProb())) {
                    person.addSkill(SkillType.S_ANTI_MECH, expLvl,
                            rskillPrefs.randomizeSkill(), bonus, mod);
                }
                person.addSkill(SkillType.S_SMALL_ARMS, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_SPACE_PILOT):
                person.addSkill(SkillType.S_PILOT_SPACE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_SPACE_CREW):
                person.addSkill(SkillType.S_TECH_VESSEL, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_SPACE_GUNNER):
                person.addSkill(SkillType.S_GUN_SPACE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_NAVIGATOR):
                person.addSkill(SkillType.S_NAV, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_MECH_TECH):
                person.addSkill(SkillType.S_TECH_MECH, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_MECHANIC):
                person.addSkill(SkillType.S_TECH_MECHANIC, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_AERO_TECH):
                person.addSkill(SkillType.S_TECH_AERO, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_BA_TECH):
                person.addSkill(SkillType.S_TECH_BA, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_ASTECH):
                person.addSkill(SkillType.S_ASTECH, 0, 0);
                break;
            case (Person.T_DOCTOR):
                person.addSkill(SkillType.S_DOCTOR, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_MEDIC):
                person.addSkill(SkillType.S_MEDTECH, 0, 0);
                break;
            case (Person.T_ADMIN_COM):
            case (Person.T_ADMIN_LOG):
            case (Person.T_ADMIN_TRA):
            case (Person.T_ADMIN_HR):
                person.addSkill(SkillType.S_ADMIN, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
        }
    }

    /**
     * If the person does not already have a bloodname, assigns a chance of having one based on
     * skill and rank. If the roll indicates there should be a bloodname, one is assigned as
     * appropriate to the person's phenotype and the player's faction.
     * 
     * @param person       The Bloodname candidate
     * @param type         The phenotype index
     */
    public void checkBloodnameAdd(Person person, int type) {
        checkBloodnameAdd(person, type, false, this.factionCode);
    }
    
    /**
     * If the person does not already have a bloodname, assigns a chance of having one based on
     * skill and rank. If the roll indicates there should be a bloodname, one is assigned as
     * appropriate to Clan and phenotype.
     * 
     * @param person       The Bloodname candidate
     * @param type         The phenotype index
     * @param factionCode  The shortName of the faction the person belongs to. Note that there
     *                     is a chance of having a Bloodname that is unique to a different Clan
     *                     as this person could have been captured.
     */
    public void checkBloodnameAdd(Person person, int type, String factionCode) {
        checkBloodnameAdd(person, type, false, factionCode);
    }
    
    /**
     * If the person does not already have a bloodname, assigns a chance of having one based on
     * skill and rank. If the roll indicates there should be a bloodname, one is assigned as
     * appropriate to the person's phenotype and the player's faction.
     * 
     * @param person       The Bloodname candidate
     * @param type         The phenotype index
     * @param ignoreDice   If true, skips the random roll and assigns a Bloodname automatically
     */
    public void checkBloodnameAdd(Person person, int type, boolean ignoreDice) {
        checkBloodnameAdd(person, type, ignoreDice, this.factionCode);
    }

    /**
     * If the person does not already have a bloodname, assigns a chance of having one based on
     * skill and rank. If the roll indicates there should be a bloodname, one is assigned as
     * appropriate to Clan and phenotype.
     * 
     * @param person       The Bloodname candidate
     * @param type         The phenotype index
     * @param ignoreDice   If true, skips the random roll and assigns a Bloodname automatically
     * @param factionCode  The shortName of the faction the person belongs to. Note that there
     *                     is a chance of having a Bloodname that is unique to a different Clan
     *                     as this person could have been captured.
     */
    public void checkBloodnameAdd(Person person, int type, boolean ignoreDice, String factionCode) {
        // Person already has a bloodname?
        if (person.getBloodname().length() > 0) {
            int result = JOptionPane.showConfirmDialog(null,
                    person.getName() + " already has the bloodname " + person.getBloodname()
                            + "\nDo you wish to remove that bloodname and generate a new one?",
                    "Already Has Bloodname", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.NO_OPTION) {
                return;
            }
        }

        // Go ahead and generate a new bloodname
        if (person.isClanner() && person.getPhenotype() != Person.PHENOTYPE_NONE) {
            int bloodnameTarget = 6;
            switch (person.getPhenotype()) {
                case Person.PHENOTYPE_MW:
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_MECH)
                            ? person.getSkill(SkillType.S_GUN_MECH).getFinalSkillValue()
                            : 13;
                    bloodnameTarget += person.hasSkill(SkillType.S_PILOT_MECH)
                            ? person.getSkill(SkillType.S_PILOT_MECH).getFinalSkillValue()
                            : 13;
                    break;
                case Person.PHENOTYPE_AERO:
                    if (type == Person.T_PROTO_PILOT) {
                        bloodnameTarget += 2 * (person.hasSkill(SkillType.S_GUN_PROTO)
                                ? person.getSkill(SkillType.S_GUN_PROTO).getFinalSkillValue()
                                : 13);

                    } else {
                        bloodnameTarget += person.hasSkill(SkillType.S_GUN_AERO)
                                ? person.getSkill(SkillType.S_GUN_AERO).getFinalSkillValue()
                                : 13;
                        bloodnameTarget += person.hasSkill(SkillType.S_PILOT_AERO)
                                ? person.getSkill(SkillType.S_PILOT_AERO).getFinalSkillValue()
                                : 13;
                    }
                    break;
                case Person.PHENOTYPE_BA:
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_BA)
                            ? person.getSkill(SkillType.S_GUN_BA).getFinalSkillValue()
                            : 13;
                    bloodnameTarget += person.hasSkill(SkillType.S_ANTI_MECH)
                            ? person.getSkill(SkillType.S_ANTI_MECH).getFinalSkillValue()
                            : 13;
                    break;
                case Person.PHENOTYPE_VEE:
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_VEE)
                            ? person.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue()
                            : 13;
                    if (type == Person.T_VTOL_PILOT) {
                        bloodnameTarget += person.hasSkill(SkillType.S_PILOT_VTOL)
                                ? person.getSkill(SkillType.S_PILOT_VTOL).getFinalSkillValue()
                                : 13;
                    } else if (type == Person.T_NVEE_DRIVER) {
                        bloodnameTarget += person.hasSkill(SkillType.S_PILOT_NVEE)
                                ? person.getSkill(SkillType.S_PILOT_NVEE).getFinalSkillValue()
                                : 13;
                    } else {
                        bloodnameTarget += person.hasSkill(SkillType.S_PILOT_GVEE)
                                ? person.getSkill(SkillType.S_PILOT_GVEE).getFinalSkillValue()
                                : 13;
                    }
                    break;
            }
            // Higher rated units are more likely to have Bloodnamed
            if (campaignOptions.useDragoonRating()) {
                IUnitRating rating = getUnitRating();
                bloodnameTarget += IUnitRating.DRAGOON_C - (campaignOptions.getUnitRatingMethod().equals(
                        mekhq.campaign.rating.UnitRatingMethod.FLD_MAN_MERCS_REV) ? rating.getUnitRatingAsInteger()
                                : rating.getModifier());
            }
            // Reavings diminish the number of available Bloodrights in later eras
            int year = getCalendar().get(Calendar.YEAR);
            if (year <= 2950)
                bloodnameTarget--;
            if (year > 3055)
                bloodnameTarget++;
            if (year > 3065)
                bloodnameTarget++;
            if (year > 3080)
                bloodnameTarget++;
            // Officers have better chance; no penalty for non-officer
            bloodnameTarget += Math.min(0, ranks.getOfficerCut() - person.getRankNumeric());

            if (Compute.d6(2) >= bloodnameTarget || ignoreDice) {
                /*
                 * The Bloodname generator has slight differences in categories that do not map
                 * easily onto Person constants
                 */
                int phenotype = Bloodname.P_GENERAL;
                switch (type) {
                    case Person.T_MECHWARRIOR:
                        phenotype = Bloodname.P_MECHWARRIOR;
                        break;
                    case Person.T_BA:
                        phenotype = Bloodname.P_ELEMENTAL;
                        break;
                    case Person.T_AERO_PILOT:
                    case Person.T_CONV_PILOT:
                        phenotype = Bloodname.P_AEROSPACE;
                        break;
                    case Person.T_SPACE_CREW:
                    case Person.T_NAVIGATOR:
                    case Person.T_SPACE_GUNNER:
                    case Person.T_SPACE_PILOT:
                        phenotype = Bloodname.P_NAVAL;
                        break;
                    case Person.T_PROTO_PILOT:
                        phenotype = Bloodname.P_PROTOMECH;
                        break;
                }
                Bloodname bloodname = Bloodname.randomBloodname(factionCode, phenotype, getGameYear());
                if (null != bloodname) {
                    person.setBloodname(bloodname.getName());
                }
            }
        }
        MekHQ.triggerEvent(new PersonChangedEvent(person));
    }

    public String rollSPA(int type, Person person) {
        ArrayList<SpecialAbility> abilityList = person.getEligibleSPAs(true);
        if (abilityList.isEmpty()) {
            return null;
        }

        // create a weighted list based on XP
        ArrayList<String> weightedList = new ArrayList<String>();
        for (SpecialAbility spa : abilityList) {
            int weight = spa.getWeight();
            while (weight > 0) {
                weightedList.add(spa.getName());
                weight--;
            }
        }
        String name = Utilities.getRandomItem(weightedList);
        if (name.equals("specialist")) {
            String special = Crew.SPECIAL_NONE;
            switch (Compute.randomInt(2)) {
                case 0:
                    special = Crew.SPECIAL_ENERGY;
                    break;
                case 1:
                    special = Crew.SPECIAL_BALLISTIC;
                    break;
                case 2:
                    special = Crew.SPECIAL_MISSILE;
                    break;
            }
            person.acquireAbility(PilotOptions.LVL3_ADVANTAGES, name, special);
        } else if (name.equals("range_master")) {
            String special = Crew.RANGEMASTER_NONE;
            switch (Compute.randomInt(2)) {
                case 0:
                    special = Crew.RANGEMASTER_MEDIUM;
                    break;
                case 1:
                    special = Crew.RANGEMASTER_LONG;
                    break;
                case 2:
                    special = Crew.RANGEMASTER_EXTREME;
                    break;
            }
            person.acquireAbility(PilotOptions.LVL3_ADVANTAGES, name,
                    special);
        } else if (name.equals("human_tro")) {
            String special = Crew.HUMANTRO_NONE;
            switch (Compute.randomInt(3)) {
                case 0:
                    special = Crew.HUMANTRO_MECH;
                    break;
                case 1:
                    special = Crew.HUMANTRO_AERO;
                    break;
                case 2:
                    special = Crew.HUMANTRO_VEE;
                    break;
                case 3:
                    special = Crew.HUMANTRO_BA;
                    break;
            }
            person.acquireAbility(PilotOptions.LVL3_ADVANTAGES, name,
                    special);
        } else if (name.equals("weapon_specialist")) {
            person.acquireAbility(PilotOptions.LVL3_ADVANTAGES, name,
                    SpecialAbility.chooseWeaponSpecialization(type, getFaction().isClan(),
                            getCampaignOptions().getTechLevel(), getCalendar().get(GregorianCalendar.YEAR)));
        } else {
            person.acquireAbility(PilotOptions.LVL3_ADVANTAGES, name, true);
        }
        return name;
    }

    public Ranks getRanks() {
        return ranks;
    }

    public void setRankSystem(int system) {
        getRanks().setRankSystem(system);
    }

    public List<String> getAllRankNamesFor(int profession) {

        List<String> retVal = new ArrayList<String>();
        for(Rank rank : getRanks().getAllRanks()) {
            int p = profession;
            // Grab rank from correct profession as needed
            while (rank.getName(p).startsWith("--") && p != Ranks.RPROF_MW) {
                if (rank.getName(p).equals("--")) {
                    p = getRanks().getAlternateProfession(p);
                } else if (rank.getName(p).startsWith("--")) {
                    p = getRanks().getAlternateProfession(rank.getName(p));
                }
            }
            if (rank.getName(p).equals("-")) {
                continue;
            }

            retVal.add(rank.getName(p));
        }
        return retVal;
    }

    public ArrayList<Force> getAllForces() {
        ArrayList<Force> allForces = new ArrayList<Force>(forceIds.values());
        return allForces;
    }

    public Finances getFinances() {
        return finances;
    }

    public ArrayList<IPartWork> getPartsNeedingServiceFor(UUID uid) {
        return getPartsNeedingServiceFor(uid, false);
    }

    public ArrayList<IPartWork> getPartsNeedingServiceFor(UUID uid, boolean onlyNotBeingWorkedOn) {
        if (null == uid) {
            return new ArrayList<IPartWork>();
        }
        Unit u = getUnit(uid);
        if (u != null) {
            if (u.isSalvage() || !u.isRepairable()) {
                return u.getSalvageableParts(onlyNotBeingWorkedOn);
            } else {
                return u.getPartsNeedingFixing(onlyNotBeingWorkedOn);
            }
        }
        return new ArrayList<IPartWork>();
    }

    public ArrayList<IAcquisitionWork> getAcquisitionsForUnit(UUID uid) {
        if (null == uid) {
            return new ArrayList<IAcquisitionWork>();
        }
        Unit u = getUnit(uid);
        if (u != null) {
            return u.getPartsNeeded();
        }
        return new ArrayList<IAcquisitionWork>();
    }

    /**
     * Use an A* algorithm to find the best path between two planets For right now, we are just going to minimize the number
     * of jumps but we could extend this to take advantage of recharge information or other variables as well Based on
     * http://www.policyalmanac.org/games/aStarTutorial.htm
     *
     * @param startKey
     * @param endKey
     * @return
     */
    public JumpPath calculateJumpPath(Planet start, Planet end) {
        if(null == start) {
            return null;
        }
        if((null == end) || start.getId().equals(end.getId())) {
            JumpPath jpath = new JumpPath();
            jpath.addPlanet(start);
            return jpath;
        }
        String startKey = start.getId();
        String endKey = end.getId();

        final DateTime now = Utilities.getDateTimeDay(calendar);
        String current = startKey;
        ArrayList<String> closed = new ArrayList<String>();
        ArrayList<String> open = new ArrayList<String>();
        boolean found = false;
        int jumps = 0;

        // we are going to through and set up some hashes that will make our
        // work easier
        // hash of parent key
        Hashtable<String, String> parent = new Hashtable<>();
        // hash of H for each planet which will not change
        Hashtable<String, Double> scoreH = new Hashtable<>();
        // hash of G for each planet which might change
        Hashtable<String, Double> scoreG = new Hashtable<>();

        for (String key : Planets.getInstance().getPlanets().keySet()) {
            scoreH.put(
                    key,
                    end.getDistanceTo(Planets.getInstance().getPlanets()
                            .get(key)));
        }
        scoreG.put(current, 0.0);
        closed.add(current);

        while (!found && jumps < 10000) {
            jumps++;
            double currentG = scoreG.get(current) + Planets.getInstance().getPlanetById(current).getRechargeTime(now);
            List<Planet> neighborKeys = Planets.getInstance().getNearbyPlanets(Planets.getInstance().getPlanetById(current), 30);
            for (Planet neighborKey : neighborKeys) {
                if (closed.contains(neighborKey.getId())) {
                    continue;
                } else if (open.contains(neighborKey.getId())) {
                    // is the current G better than the existing G
                    if (currentG < scoreG.get(neighborKey.getId())) {
                        // then change G and parent
                        scoreG.put(neighborKey.getId(), currentG);
                        parent.put(neighborKey.getId(), current);
                    }
                } else {
                    // put the current G for this one in memory
                    scoreG.put(neighborKey.getId(), currentG);
                    // put the parent in memory
                    parent.put(neighborKey.getId(), current);
                    open.add(neighborKey.getId());
                }
            }
            String bestMatch = null;
            double bestF = Double.POSITIVE_INFINITY;
            for (String possible : open) {
                // calculate F
                double currentF = scoreG.get(possible) + scoreH.get(possible);
                if (currentF < bestF) {
                    bestMatch = possible;
                    bestF = currentF;
                }
            }
            current = bestMatch;
            if(null == current) {
                // We're done - probably failed to find anything
                break;
            }
            closed.add(current);
            open.remove(current);
            if (current.equals(endKey)) {
                found = true;
            }
        }
        // now we just need to back up from the last current by parents until we
        // hit null
        ArrayList<Planet> path = new ArrayList<Planet>();
        String nextKey = current;
        while (null != nextKey) {
            path.add(Planets.getInstance().getPlanets().get(nextKey));
            // MekHQApp.logMessage(nextKey);
            nextKey = parent.get(nextKey);

        }
        // now reverse the direaction
        JumpPath finalPath = new JumpPath();
        for (int i = (path.size() - 1); i >= 0; i--) {
            finalPath.addPlanet(path.get(i));
        }
        return finalPath;
    }

    public List<Planet> getAllReachablePlanetsFrom(Planet planet) {
        return Planets.getInstance().getNearbyPlanets(planet, 30);
    }

    /**
     * This method calculates the cost per jump for interstellar travel. It operates by fitting the part
     * of the force not transported in owned DropShips into a number of prototypical DropShips of a few
     * standard configurations, then adding the JumpShip charges on top. It remains fairly hacky, but
     * improves slightly on the prior implementation as far as following the rulebooks goes.
     *
     * It can be used to calculate total travel costs in the style of FM:Mercs (excludeOwnTransports
     * and campaignOpsCosts set to false), to calculate leased/rented travel costs only in the style
     * of FM:Mercs (excludeOwnTransports true, campaignOpsCosts false), or to calculate travel costs
     * for CampaignOps-style costs (excludeOwnTransports true, campaignOpsCosts true).
     *
     *  @param excludeOwnTransports If true, do not display maintenance costs in the calculated travel cost.
     * @param campaignOpsCosts If true, use the Campaign Ops method for calculating travel cost. (DropShip monthly fees
     *                         of 0.5% of purchase cost, 100,000 C-bills per collar.)
     */
    @SuppressWarnings("unused") // FIXME: Waiting for Dylan to finish re-writing
    public long calculateCostPerJump(boolean excludeOwnTransports, boolean campaignOpsCosts) {
        int collarCost = (campaignOpsCosts ? 100000 : 50000);

        // first we need to get the total number of units by type
        int nMech = getNumberOfUnitsByType(Entity.ETYPE_MECH);
        int nLVee = getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true);
        int nHVee = getNumberOfUnitsByType(Entity.ETYPE_TANK);
        int nAero = getNumberOfUnitsByType(Entity.ETYPE_AERO);
        int nSC = getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT);
        int nCF = getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER);
        int nBA = getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR);
        int nMechInf = 0;
        int nMotorInf = 0;
        int nFootInf = 0;
        int nProto = getNumberOfUnitsByType(Entity.ETYPE_PROTOMECH);
        int nDropship = getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
        int nCollars = getTotalDockingCollars();
        double nCargo = getTotalCargoCapacity(); // ignoring refrigerated/insulated/etc.

        // get cargo tonnage including parts in transit, then get mothballed unit
        // tonnage
        double carriedCargo = getCargoTonnage(true, false) + getCargoTonnage(false, true);

        // calculate the number of units left untransported
        int noMech = Math.max(nMech - getOccupiedBays(Entity.ETYPE_MECH), 0);
        int noDS = Math.max(nDropship - getOccupiedBays(Entity.ETYPE_DROPSHIP), 0);
        int noSC = Math.max(nSC - getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        int noCF = Math.max(nCF - getOccupiedBays(Entity.ETYPE_CONV_FIGHTER), 0);
        int noASF = Math.max(nAero - getOccupiedBays(Entity.ETYPE_AERO), 0);
        int nolv = Math.max(nLVee - getOccupiedBays(Entity.ETYPE_TANK, true), 0);
        int nohv = Math.max(nHVee - getOccupiedBays(Entity.ETYPE_TANK), 0);
        int noinf = Math.max(getNumberOfUnitsByType(Entity.ETYPE_INFANTRY) - getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int noBA = Math.max(nBA - getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        int noProto = Math.max(nProto - getOccupiedBays(Entity.ETYPE_PROTOMECH), 0);
        int freehv = Math.max(getTotalHeavyVehicleBays() - getOccupiedBays(Entity.ETYPE_TANK), 0);
        int freeinf = Math.max(getTotalInfantryBays() - getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int freeba = Math.max(getTotalBattleArmorBays() - getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        int freeSC = Math.max(getTotalSmallCraftBays() - getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        int noCargo = (int) Math.ceil(Math.max(carriedCargo - nCargo, 0));

        int newNoASF = Math.max(noASF - freeSC, 0);
        int placedASF = Math.max(noASF - newNoASF, 0);
        freeSC -= placedASF;

        int newNolv = Math.max(nolv - freehv, 0);
        int placedlv = Math.max(nolv - newNolv, 0);
        freehv -= placedlv;
        int noVehicles = (nohv + newNolv);

        long dropshipCost = 0;
        // The cost-figuring process: using prototypical dropships, figure out how
        // many collars are required. Charge for the prototypical dropships and
        // the docking collar, based on the rules selected. Allow prototypical
        // dropships to be leased in 1/2 increments; designs of roughly 1/2
        // size exist for all of the prototypical variants chosen.

        // DropShip costs are for the duration of the trip for FM:Mercs rules,
        // and per month for Campaign Ops. The prior implementation here assumed
        // the FM:Mercs costs were per jump, which seems reasonable. To avoid having
        // to add a bunch of code to remember the total length of the current
        // jump path, CamOps costs are normalized to per-jump, using 175 hours charge
        // time as a baseline.

        // Roughly an Overlord
        int largeDropshipMechCapacity = 36;
        int largeMechDropshipASFCapacity = 6;
        int largeMechDropshipCargoCapacity = 120;
        long largeMechDropshipCost = (campaignOpsCosts ? (long)(1750000 / 4.2) : 400000);

        // Roughly a Union
        int averageDropshipMechCapacity = 12;
        int mechDropshipASFCapacity = 2;
        int mechDropshipCargoCapacity = 75;
        long mechDropshipCost = (campaignOpsCosts ? (long)(1450000 / 4.2) : 150000);

        // Roughly a Leopard CV
        int averageDropshipASFCapacity = 6;
        int asfDropshipCargoCapacity = 90;
        long asfDropshipCost = (campaignOpsCosts ? (long)(900000 / 4.2) : 80000);

        // Roughly a Triumph
        int largeDropshipVehicleCapacity = 50;
        int largeVehicleDropshipCargoCapacity = 750;
        long largeVehicleDropshipCost = (campaignOpsCosts ? (long)(1750000 / 4.2) : 430000);

        // Roughly a Gazelle
        int averageDropshipVehicleCapacity = 15;
        int vehicleDropshipCargoCapacity = 65;
        long vehicleDropshipCost = (campaignOpsCosts ? (long)(900000 / 4.2): 40000);

        // Roughly a Mule
        int largeDropshipCargoCapacity = 8000;
        long largeCargoDropshipCost = (campaignOpsCosts ? (long)(750000 / 4.2) : 800000);

        // Roughly a Buccaneer
        int averageDropshipCargoCapacity = 2300;
        long cargoDropshipCost = (campaignOpsCosts ? (long)(550000 / 4.2) : 250000);

        int mechCollars = 0;
        double leasedLargeMechDropships = 0;
        double leasedAverageMechDropships = 0;

        int asfCollars = 0;
        double leasedAverageASFDropships = 0;

        int vehicleCollars = 0;
        double leasedLargeVehicleDropships = 0;
        double leasedAverageVehicleDropships = 0;

        int cargoCollars = 0;
        double leasedLargeCargoDropships = 0;
        double leasedAverageCargoDropships = 0;

        int leasedASFCapacity = 0;
        int leasedCargoCapacity = 0;

        // For each type we're concerned with, calculate the number of dropships needed
        // to transport the force. Smaller dropships are represented by half-dropships.

        // If we're transporting more than a company, Overlord analogues are more efficient.
        if(noMech > 12) {
            leasedLargeMechDropships = noMech / largeDropshipMechCapacity;
            noMech -= leasedLargeMechDropships * largeDropshipMechCapacity;
            mechCollars += leasedLargeMechDropships;

            // If there's more than a company left over, lease another Overlord. Otherwise
            // fall through and get a Union.
            if(noMech > 12) {
                leasedLargeMechDropships += 1;
                noMech -= largeDropshipMechCapacity;
                mechCollars += 1;
            }

            leasedASFCapacity += leasedLargeMechDropships * largeMechDropshipASFCapacity;
            leasedCargoCapacity += largeMechDropshipCargoCapacity;
        }

        // Unions
        if(noMech > 0) {
            leasedAverageMechDropships = noMech / averageDropshipMechCapacity;
            noMech -= leasedAverageMechDropships * averageDropshipMechCapacity;
            mechCollars += leasedAverageMechDropships;

            // If we can fit in a smaller dropship, lease one of those instead.
            if(noMech > 0 && noMech < (averageDropshipMechCapacity / 2)) {
                leasedAverageMechDropships += 0.5;
                mechCollars += 1;
            }
            else if(noMech > 0){
                leasedAverageMechDropships += 1;
                mechCollars += 1;
            }

            // Our Union-ish dropship can carry some ASFs and cargo.
            leasedASFCapacity += (int) Math.floor(leasedAverageMechDropships * mechDropshipASFCapacity);
            leasedCargoCapacity += (int) Math.floor(leasedAverageMechDropships * mechDropshipCargoCapacity);
        }

        // Leopard CVs
        if(noASF > leasedASFCapacity) {
            noASF -= leasedASFCapacity;

            if(noASF > 0) {
                leasedAverageASFDropships = noASF / averageDropshipASFCapacity;
                noASF -= leasedAverageASFDropships * averageDropshipASFCapacity;
                asfCollars += leasedAverageASFDropships;

                if (noASF > 0 && noASF < (averageDropshipASFCapacity / 2)) {
                    leasedAverageASFDropships += 0.5;
                    asfCollars += 1;
                }
                else if (noASF > 0) {
                    leasedAverageASFDropships += 1;
                    asfCollars += 1;
                }
            }

            // Our Leopard-ish dropship can carry some cargo.
            leasedCargoCapacity += (asfDropshipCargoCapacity * leasedAverageASFDropships);
        }

        // Triumphs
        if(noVehicles > averageDropshipVehicleCapacity) {
            leasedLargeVehicleDropships = noVehicles / largeDropshipVehicleCapacity;
            noVehicles -= leasedLargeVehicleDropships * largeDropshipVehicleCapacity;
            vehicleCollars += leasedLargeVehicleDropships;

            if(noVehicles > averageDropshipVehicleCapacity) {
                leasedLargeVehicleDropships += 1;
                noVehicles -= largeDropshipVehicleCapacity;
                vehicleCollars += 1;
            }

            leasedCargoCapacity += leasedLargeVehicleDropships * largeVehicleDropshipCargoCapacity;
        }

        // Gazelles
        if(noVehicles > 0) {
            leasedAverageVehicleDropships = (nohv + newNolv) / averageDropshipVehicleCapacity;
            noVehicles = (int)((nohv + newNolv) - leasedAverageVehicleDropships * averageDropshipVehicleCapacity);
            vehicleCollars += leasedAverageVehicleDropships;

            // Gazelles are pretty minimal, so no half-measures.
            if(noVehicles > 0) {
                leasedAverageVehicleDropships += 1;
                noVehicles -= averageDropshipVehicleCapacity;
                vehicleCollars += 1;
            }

            // Our Gazelle-ish dropship can carry some cargo.
            leasedCargoCapacity += (vehicleDropshipCargoCapacity * leasedAverageVehicleDropships);
        }

        // Do we have any leftover cargo?
        noCargo -= leasedCargoCapacity;

        // Mules
        if(noCargo > averageDropshipCargoCapacity) {
            leasedLargeCargoDropships = noCargo / largeDropshipCargoCapacity;
            noCargo -= leasedLargeCargoDropships * largeDropshipCargoCapacity;
            cargoCollars += leasedLargeCargoDropships;

            if(noCargo > averageDropshipCargoCapacity) {
                leasedLargeCargoDropships += 1;
                noCargo -= largeDropshipCargoCapacity;
                cargoCollars += 1;
            }
        }

        // Buccaneers
        if(noCargo > 0) {
            leasedAverageCargoDropships = noCargo / averageDropshipCargoCapacity;
            cargoCollars += leasedAverageCargoDropships;
            noCargo -= leasedAverageCargoDropships * averageDropshipCargoCapacity;

            if(noCargo > 0 && noCargo < (averageDropshipCargoCapacity / 2)) {
                leasedAverageCargoDropships += 0.5;
                cargoCollars += 1;
            }
            else if(noCargo > 0) {
                leasedAverageCargoDropships += 1;
                cargoCollars += 1;
            }
        }

        dropshipCost = (long) (leasedAverageMechDropships * mechDropshipCost);
        dropshipCost += (long) (leasedLargeMechDropships * largeMechDropshipCost);

        dropshipCost += (long) (leasedAverageASFDropships * asfDropshipCost);

        dropshipCost += (long) (leasedAverageVehicleDropships * vehicleDropshipCost);
        dropshipCost += (long) (leasedLargeVehicleDropships * largeVehicleDropshipCost);

        dropshipCost += (long) (leasedAverageCargoDropships * cargoDropshipCost);
        dropshipCost += (long) (leasedLargeCargoDropships * largeCargoDropshipCost);

        // Smaller/half-dropships are cheaper to rent, but still take one collar each
        int collarsNeeded = mechCollars + asfCollars + vehicleCollars + cargoCollars;

        // add owned dropships
        collarsNeeded += nDropship;

        // now factor in owned jumpships
        collarsNeeded = Math.max(0, collarsNeeded - nCollars);

        long totalCost = dropshipCost + collarsNeeded * collarCost;

        // FM:Mercs reimburses for owned transport (CamOps handles it in peacetime costs)
        if(!excludeOwnTransports) {
            long ownDropshipCost = 0;
            long ownJumpshipCost = 0;
            for(Unit u : getUnits()) {
                if(!u.isMothballed()) {
                    Entity e = u.getEntity();
                    if((e.getEntityType() & Entity.ETYPE_DROPSHIP) != 0) {
                        ownDropshipCost += u.getMechCapacity() * (mechDropshipCost / averageDropshipMechCapacity);
                        ownDropshipCost += u.getASFCapacity() * (asfDropshipCost / averageDropshipASFCapacity);
                        ownDropshipCost += (u.getHeavyVehicleCapacity() + u.getLightVehicleCapacity()) * (vehicleDropshipCost / averageDropshipVehicleCapacity);
                        ownDropshipCost += u.getCargoCapacity() * (cargoDropshipCost / averageDropshipCargoCapacity);
                    }
                    else if((e.getEntityType() & Entity.ETYPE_JUMPSHIP) != 0) {
                        ownJumpshipCost += e.getDockingCollars().size() * collarCost;
                    }
                }
            }

            totalCost = totalCost + ownDropshipCost + ownJumpshipCost;
        }

        return totalCost;
    }

    public void personUpdated(Person p) {
        Unit u = getUnit(p.getUnitId());
        if (null != u) {
            u.resetPilotAndEntity();
        }
        MekHQ.triggerEvent(new PersonChangedEvent(p));
    }

    public TargetRoll getTargetFor(IPartWork partWork, Person tech) {
        Skill skill = tech.getSkillForWorkingOn(partWork);
        int modePenalty = partWork.getMode().expReduction;
        if (null != partWork.getUnit() && !partWork.getUnit().isAvailable(partWork instanceof Refit)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "This unit is not currently available!");
        }
        if (partWork.getTeamId() != null
                && !partWork.getTeamId().equals(tech.getId())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "Already being worked on by another team");
        }
        if (null == skill) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "Assigned tech does not have the right skills");
        }
        if (!getCampaignOptions().isDestroyByMargin()
                && partWork.getSkillMin() > (skill.getExperienceLevel() - modePenalty)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "Task is beyond this tech's skill level");
        }
        if (partWork.getSkillMin() > SkillType.EXP_ELITE) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is impossible.");
        }
        if (!partWork.needsFixing() && !partWork.isSalvaging()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is not needed.");
        }
        if (partWork instanceof MissingPart
                && null == ((MissingPart) partWork).findReplacement(false)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Part not available.");
        }
        if (!(partWork instanceof Refit) && tech.getMinutesLeft() <= 0
                && (!isOvertimeAllowed() || tech.getOvertimeLeft() <= 0)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "No time left.");
        }
        String notFixable = partWork.checkFixable();
        if (null != notFixable) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, notFixable);
        }
        // if this is an infantry refit, then automatic success
        if (partWork instanceof Refit && null != partWork.getUnit()
                && partWork.getUnit().getEntity() instanceof Infantry
                && !(partWork.getUnit().getEntity() instanceof BattleArmor)) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS,
                    "infantry refit");
        }

        //if we are using the MoF rule, then we will ignore mode penalty here
        //and instead assign it as a straight penalty
        if (getCampaignOptions().isDestroyByMargin()) {
            modePenalty = 0;
        }

        // this is ugly, if the mode penalty drops you to green, you drop two
        // levels instead of two
        int value = skill.getFinalSkillValue() + modePenalty;
        if (modePenalty > 0
                && SkillType.EXP_GREEN == (skill.getExperienceLevel() - modePenalty)) {
            value++;
        }
        TargetRoll target = new TargetRoll(value,
                SkillType.getExperienceLevelName(skill.getExperienceLevel() - modePenalty));
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            return target;
        }

        target.append(partWork.getAllMods(tech));

        if (getCampaignOptions().useEraMods()) {
            target.addModifier(getFaction().getEraMod(getGameYear()), "era");
        }

        boolean isOvertime = false;
        if (isOvertimeAllowed()
                && (tech.isTaskOvertime(partWork) || partWork.hasWorkedOvertime())) {
            target.addModifier(3, "overtime");
            isOvertime = true;
        }

        int minutes = partWork.getTimeLeft();
        if (minutes > tech.getMinutesLeft()) {
            if (isOvertimeAllowed()) {
                if (minutes > (tech.getMinutesLeft() + tech.getOvertimeLeft())) {
                    minutes = tech.getMinutesLeft() + tech.getOvertimeLeft();
                }
            } else {
                minutes = tech.getMinutesLeft();
            }
        }
        int helpMod = 0;
        if (null != partWork.getUnit() && partWork.getUnit().isSelfCrewed()) {
            int hits = 0;
            if (null != partWork.getUnit().getEntity().getCrew()) {
                hits = partWork.getUnit().getEntity().getCrew().getHits();
            } else {
                hits = 6;
            }
            helpMod = getShorthandedModForCrews(hits);
        } else {
            int helpers = getAvailableAstechs(minutes, isOvertime);
            helpMod = getShorthandedMod(helpers, false);
            // we may have just gone overtime with our helpers
            if (!isOvertime && astechPoolMinutes < (minutes * helpers)) {
                target.addModifier(3, "overtime astechs");
            }
        }
        if (partWork.getShorthandedMod() > helpMod) {
            helpMod = partWork.getShorthandedMod();
        }
        if (helpMod > 0) {
            target.addModifier(helpMod, "shorthanded");
        }
        return target;
    }

    public TargetRoll getTargetForMaintenance(IPartWork partWork, Person tech) {
        int value = 10;
        String skillLevel = "Unmaintained";
        if (null != tech) {
            Skill skill = tech.getSkillForWorkingOn(partWork);
            if (null != skill) {
                value = skill.getFinalSkillValue();
                skillLevel = SkillType.getExperienceLevelName(skill
                        .getExperienceLevel());
            }
        }

        TargetRoll target = new TargetRoll(value, skillLevel);
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            return target;
        }

        target.append(partWork.getAllModsForMaintenance());

        if (getCampaignOptions().useEraMods()) {
            target.addModifier(getFaction().getEraMod(getGameYear()), "era");
        }

        if (null != partWork.getUnit() && null != tech) {
            // we have no official rules for what happens when a tech is only
            // assigned
            // for part of the maintenance cycle, so we will create our own
            // penalties
            if (partWork.getUnit().getMaintainedPct() < .5) {
                target.addModifier(2, "partial maintenance");
            } else if (partWork.getUnit().getMaintainedPct() < 1) {
                target.addModifier(1, "partial maintenance");
            }

            // the astech issue is crazy, because you can actually be better off
            // not maintaining
            // than going it short-handed, but that is just the way it is.
            // Still, there is also some fuzziness about what happens if you are
            // short astechs
            // for part of the cycle. We will keep keep track of the total
            // "astech days" used over
            // the cycle and take the average per day rounding down as our team
            // size
            int helpMod = 0;
            if (null != partWork.getUnit() && partWork.getUnit().isSelfCrewed()) {
                int hits = 0;
                if (null != partWork.getUnit().getEntity().getCrew()) {
                    hits = partWork.getUnit().getEntity().getCrew().getHits();
                } else {
                    hits = 6;
                }
                helpMod = getShorthandedModForCrews(hits);
            } else {
                int helpers = partWork.getUnit().getAstechsMaintained();
                helpMod = getShorthandedMod(helpers, false);
            }
            if (helpMod > 0) {
                target.addModifier(helpMod, "shorthanded");
            }
        }

        return target;
    }

    public TargetRoll getTargetForAcquisition(IAcquisitionWork acquisition,
            Person person) {
        return getTargetForAcquisition(acquisition, person, true);
    }

    public TargetRoll getTargetForAcquisition(IAcquisitionWork acquisition,
            Person person, boolean checkDaysToWait) {
        if (getCampaignOptions().getAcquisitionSkill().equals(
                CampaignOptions.S_AUTO)) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS,
                    "Automatic Success");
        }
        if (null == person) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "No one on your force is capable of acquiring parts");
        }
        Skill skill = person.getSkillForWorkingOn(acquisition,
                getCampaignOptions().getAcquisitionSkill());
        if (null != getShoppingList().getShoppingItem(
                acquisition.getNewEquipment())
                && checkDaysToWait) {
            return new TargetRoll(
                    TargetRoll.AUTOMATIC_FAIL,
                    "You must wait until the new cycle to check for this part. Further attempts will be added to the shopping list.");
        }
        if (acquisition.getTechBase() == Part.T_CLAN
                && !getCampaignOptions().allowClanPurchases()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "You cannot acquire clan parts");
        }
        if (acquisition.getTechBase() == Part.T_IS
                && !getCampaignOptions().allowISPurchases()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "You cannot acquire inner sphere parts");
        }
        if (getCampaignOptions().getTechLevel() < Utilities
                .getSimpleTechLevel(acquisition.getTechLevel())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "You cannot acquire parts of this tech level");
        }
        if(getCampaignOptions().limitByYear()
                && !acquisition.isIntroducedBy(getGameYear(), useClanTechBase(), getTechFaction())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "It has not been invented yet!");
        }
        if(getCampaignOptions().disallowExtinctStuff() &&
                (acquisition.isExtinctIn(getGameYear(), useClanTechBase(), getTechFaction())
                        || acquisition.getAvailability() == EquipmentType.RATING_X)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "It is extinct!");
        }
        if (getCampaignOptions().getUseAtB() &&
                getCampaignOptions().getRestrictPartsByMission() && acquisition instanceof Part) {
            int partAvailability = ((Part) acquisition).getAvailability();
            EquipmentType et = null;
            if (acquisition instanceof EquipmentPart) {
                et = ((EquipmentPart) acquisition).getType();
            } else if (acquisition instanceof MissingEquipmentPart) {
                et = ((MissingEquipmentPart) acquisition).getType();
            }

            /*
             * Even if we can acquire Clan parts, they have a minimum availability of F for
             * non-Clan units
             */
            if (acquisition.getTechBase() == Part.T_CLAN && !getFaction().isClan()) {
                partAvailability = Math.max(partAvailability, EquipmentType.RATING_F);
            } else if (et != null) {
                /*
                 * AtB rules do not simply affect difficulty of obtaining parts, but whether
                 * they can be obtained at all. Changing the system to use availability codes
                 * can have a serious effect on game play, so we apply a few tweaks to keep some
                 * of the more basic items from becoming completely unobtainable, while applying
                 * a minimum for non-flamer energy weapons, which was the reason this rule was
                 * included in AtB to begin with.
                 */
                if (et instanceof megamek.common.weapons.lasers.EnergyWeapon
                        && !(et instanceof megamek.common.weapons.flamers.FlamerWeapon)
                        && partAvailability < EquipmentType.RATING_C) {
                    partAvailability = EquipmentType.RATING_C;
                }
                if (et instanceof megamek.common.weapons.autocannons.ACWeapon) {
                    partAvailability -= 2;
                }
                if (et instanceof megamek.common.weapons.gaussrifles.GaussWeapon
                        || et instanceof megamek.common.weapons.flamers.FlamerWeapon) {
                    partAvailability--;
                }
                if (et instanceof megamek.common.AmmoType) {
                    switch (((megamek.common.AmmoType) et).getAmmoType()) {
                        case megamek.common.AmmoType.T_AC:
                            partAvailability -= 2;
                            break;
                        case megamek.common.AmmoType.T_GAUSS:
                            partAvailability -= 1;
                    }
                    if (((megamek.common.AmmoType) et).getMunitionType() == megamek.common.AmmoType.M_STANDARD) {
                        partAvailability--;
                    }
                }

            }

            if ((getCalendar().get(Calendar.YEAR) < 2950
                    || getCalendar().get(Calendar.YEAR) > 3040)
                    && (acquisition instanceof Armor || acquisition instanceof MissingMekActuator
                            || acquisition instanceof mekhq.campaign.parts.MissingMekCockpit
                            || acquisition instanceof mekhq.campaign.parts.MissingMekLifeSupport
                            || acquisition instanceof mekhq.campaign.parts.MissingMekLocation
                            || acquisition instanceof mekhq.campaign.parts.MissingMekSensor)) {
                partAvailability--;
            }

            if (partAvailability > findAtBPartsAvailabilityLevel(acquisition)) {
                return new TargetRoll(TargetRoll.IMPOSSIBLE,
                        "This part is not currently available to your unit.");
            }
        }
        TargetRoll target = new TargetRoll(skill.getFinalSkillValue(),
                SkillType.getExperienceLevelName(skill.getExperienceLevel()));// person.getTarget(Modes.MODE_NORMAL);
        target.append(acquisition.getAllAcquisitionMods());
        return target;
    }

    public AtBContract getAttachedAtBContract(Unit unit) {
        if (null != unit && null != lances.get(unit.getForceId())) {
            return lances.get(unit.getForceId()).getContract(this);
        }
        return null;
    }

    /**
     * AtB: count all available bonus parts
     * @return the total <code>int</code> number of bonus parts for all active contracts
     */
    public int totalBonusParts() {
        int retVal = 0;
        for (Mission m : getMissions()) {
            if (m.isActive() && m instanceof AtBContract) {
                retVal += ((AtBContract) m).getNumBonusParts();
            }
        }
        return retVal;
    }

    private int findAtBPartsAvailabilityLevel(IAcquisitionWork acquisition) {
        AtBContract contract = getAttachedAtBContract(acquisition.getUnit());
        /*
         * If the unit is not assigned to a contract, use the least restrictive active
         * contract
         */
        for (Mission m : getMissions()) {
            if (m.isActive() && m instanceof AtBContract) {
                if (null == contract
                        || ((AtBContract) m).getPartsAvailabilityLevel() > contract.getPartsAvailabilityLevel()) {
                    contract = (AtBContract) m;
                }
            }
        }
        if (null != contract) {
            return contract.getPartsAvailabilityLevel();
        }
        /* If contract is still null, the unit is not in a contract. */
        Person adminLog = findBestInRole(Person.T_ADMIN_LOG, SkillType.S_ADMIN);
        int adminLogExp = (adminLog == null) ? SkillType.EXP_ULTRA_GREEN
                : adminLog.getSkill(SkillType.S_ADMIN).getExperienceLevel();
        return getUnitRatingMod() + adminLogExp - SkillType.EXP_REGULAR;
    }

    public void resetAstechMinutes() {
        astechPoolMinutes = 480 * getNumberPrimaryAstechs() + 240
                * getNumberSecondaryAstechs();
        astechPoolOvertime = 240 * getNumberPrimaryAstechs() + 120
                * getNumberSecondaryAstechs();
    }

    public int getAstechPoolMinutes() {
        return astechPoolMinutes;
    }

    public int getAstechPoolOvertime() {
        return astechPoolOvertime;
    }

    public int getPossibleAstechPoolMinutes() {
        return 480 * getNumberPrimaryAstechs() + 240 * getNumberSecondaryAstechs();
    }

    public int getPossibleAstechPoolOvertime() {
        return 240 * getNumberPrimaryAstechs() + 120 * getNumberSecondaryAstechs();
    }

    public int getAstechPool() {
        return astechPool;
    }

    public int getMedicPool() {
        return medicPool;
    }

    public void increaseAstechPool(int i) {
        astechPool += i;
        astechPoolMinutes += (480 * i);
        astechPoolOvertime += (240 * i);
        MekHQ.triggerEvent(new AstechPoolChangedEvent(this, i));
    }

    public void decreaseAstechPool(int i) {
        astechPool = Math.max(0, astechPool - i);
        // always assume that we fire the ones who have not yet worked
        astechPoolMinutes = Math.max(0, astechPoolMinutes - 480 * i);
        astechPoolOvertime = Math.max(0, astechPoolOvertime - 240 * i);
        MekHQ.triggerEvent(new AstechPoolChangedEvent(this, -i));
    }

    public int getNumberAstechs() {
        return getNumberPrimaryAstechs() + getNumberSecondaryAstechs();
    }

    public int getNumberPrimaryAstechs() {
        int astechs = getAstechPool();
        for (Person p : getPersonnel()) {
            if ((p.getPrimaryRole() == Person.T_ASTECH) && p.isActive()
                    && !p.isDeployed()) {
                astechs++;
            }
        }
        return astechs;
    }

    public int getNumberSecondaryAstechs() {
        int astechs = 0;
        for (Person p : getPersonnel()) {
            if ((p.getSecondaryRole() == Person.T_ASTECH) && p.isActive()
                    && !p.isDeployed()) {
                astechs++;
            }
        }
        return astechs;
    }

    public int getAvailableAstechs(int minutes, boolean alreadyOvertime) {
        int availableHelp = (int) Math.floor(((double) astechPoolMinutes)
                / minutes);
        if (isOvertimeAllowed() && availableHelp < 6) {
            // if we are less than fully staffed, then determine whether
            // we should dip into overtime or just continue as short-staffed
            int shortMod = getShorthandedMod(availableHelp, false);
            int remainingMinutes = astechPoolMinutes - availableHelp * minutes;
            int extraHelp = (remainingMinutes + astechPoolOvertime) / minutes;
            int helpNeeded = 6 - availableHelp;
            if (alreadyOvertime && shortMod > 0) {
                // then add whatever we can
                availableHelp += extraHelp;
            } else if (shortMod > 3) {
                // only dip in if we can bring ourselves up to full
                if (extraHelp >= helpNeeded) {
                    availableHelp = 6;
                }
            }
        }
        if (availableHelp > 6) {
            availableHelp = 6;
        }
        if (availableHelp > getNumberAstechs()) {
            return getNumberAstechs();
        }
        return availableHelp;
    }

    public int getShorthandedMod(int availableHelp, boolean medicalStaff) {
        if (medicalStaff) {
            availableHelp += 2;
        }
        int helpMod = 0;
        if (availableHelp == 0) {
            helpMod = 4;
        } else if (availableHelp == 1) {
            helpMod = 3;
        } else if (availableHelp < 4) {
            helpMod = 2;
        } else if (availableHelp < 6) {
            helpMod = 1;
        }
        return helpMod;
    }

    public int getShorthandedModForCrews(int hits) {
        int helpMod = 0;
        if (hits >= 5) {
            helpMod = 4;
        } else if (hits == 4) {
            helpMod = 3;
        } else if (hits == 3) {
            helpMod = 2;
        } else if (hits > 0) {
            helpMod = 1;
        }
        return helpMod;
    }

    public int getMedicsPerDoctor() {
        int ndocs = getDoctors().size();
        int nmedics = getNumberMedics();
        if (ndocs == 0) {
            return 0;
        }
        // TODO: figure out what to do with fractions
        return Math.min(nmedics / ndocs, 4);
    }

    public int getNumberMedics() {
        int medics = medicPool;
        for (Person p : getPersonnel()) {
            if ((p.getPrimaryRole() == Person.T_MEDIC || p.getSecondaryRole() == Person.T_MEDIC)
                    && p.isActive() && !p.isDeployed()) {
                medics++;
            }
        }
        return medics;
    }

    public void increaseMedicPool(int i) {
        medicPool += i;
        MekHQ.triggerEvent(new MedicPoolChangedEvent(this, i));
    }

    public void decreaseMedicPool(int i) {
        medicPool = Math.max(0, medicPool - i);
        MekHQ.triggerEvent(new MedicPoolChangedEvent(this, -i));
    }

    public void changePrisonerStatus(Person p, int status) {
        switch (status) {
            case Person.PRISONER_NOT:
                p.setFreeMan();
                if (p.getRankNumeric() < 0) {
                    changeRank(p, 0, false);
                }
                p.addLogEntry(getDate(), "Freed");
                if (getCampaignOptions().getUseTimeInService()) {
                    p.setRecruitment((GregorianCalendar) getCalendar().clone());
                }
                break;
            case Person.PRISONER_YES:
                if (p.getRankNumeric() > 0) {
                    changeRank(p, Ranks.RANK_PRISONER, true); // They don't get to have a rank. Their
                    // rank is Prisoner or Bondsman.
                }
                p.setPrisoner();
                p.addLogEntry(getDate(), "Made Prisoner");
                if (getCampaignOptions().getUseTimeInService()) {
                    p.setRecruitment(null);
                }
                break;
            case Person.PRISONER_BONDSMAN:
                if (p.getRankNumeric() > 0) {
                    changeRank(p, Ranks.RANK_BONDSMAN, true); // They don't get to have a rank. Their
                    // rank is Prisoner or Bondsman.
                }
                p.setBondsman();
                p.addLogEntry(getDate(), "Made Bondsman");
                if (getCampaignOptions().getUseTimeInService()) {
                    p.setRecruitment(null);
                }
                break;
            default:
                break;
        }
        if (p.isBondsman() || p.isPrisoner()) {
            Unit u = getUnit(p.getUnitId());
            if (u != null) {
                u.remove(p, true);
            }
        }
        MekHQ.triggerEvent(new PersonChangedEvent(p));
    }

    public void changeStatus(Person person, int status) {
        Unit u = getUnit(person.getUnitId());
        if (status == Person.S_KIA) {
            person.addLogEntry(getDate(), "Killed in action");
            // Don't forget to tell the spouse
            if (person.getSpouseID() != null) {
                Person spouse = person.getSpouse();
                spouse.addLogEntry(getDate(), "Spouse, " + person.getName() + ", killed in action");
                spouse.setSpouseID(null);
            }
            // set the deathday
            person.setDeathday((GregorianCalendar) calendar.clone());
        } else if (person.getStatus() == Person.S_KIA) {
            // remove deathdates for resurrection
            person.setDeathday(null);
        }
        if (status == Person.S_MIA) {
            person.addLogEntry(getDate(), "Missing in action");
        }
        if (status == Person.S_RETIRED) {
            person.addLogEntry(getDate(), "Retired from active duty");
        }
        if (status == Person.S_ACTIVE && person.getStatus() == Person.S_MIA) {
            person.addLogEntry(getDate(), "Recovered from MIA status");
        }
        person.setStatus(status);
        if (status != Person.S_ACTIVE) {
            person.setDoctorId(null, getCampaignOptions()
                    .getNaturalHealingWaitingPeriod());
            // If we're assigned to a unit, remove us from it
            if (null != u) {
                u.remove(person, true);
            }
            // If we're assigned as a tech for any unit, remove us from it/them
            if (!person.getTechUnitIDs().isEmpty()) {
                @SuppressWarnings("unchecked") // Broken assed Java returning Object from clone
                ArrayList<UUID> techIDs = (ArrayList<UUID>) person.getTechUnitIDs().clone();
                for (UUID tuuid : techIDs) {
                    Unit t = getUnit(tuuid);
                    t.remove(person, true);
                }
            }
            // If we're assigned to any repairs or refits, remove that assignment
            for (Part part : getParts()) {
                if (person.getId().equals(part.getTeamId())) {
                    part.cancelAssignment();
                }
            }
        }
        MekHQ.triggerEvent(new PersonChangedEvent(person));
    }

    public void changeRank(Person person, int rank, boolean report) {
        changeRank(person, rank, 0, report);
    }

    public void changeRank(Person person, int rank, int rankLevel, boolean report) {
        int oldRank = person.getRankNumeric();
        int oldRankLevel = person.getRankLevel();
        person.setRankNumeric(rank);
        person.setRankLevel(rankLevel);
        personUpdated(person);
        MekHQ.triggerEvent(new PersonChangedEvent(person));
        if (report) {
            if (rank > oldRank || (rank == oldRank && rankLevel > oldRankLevel)) {
                person.addLogEntry(getDate(), "Promoted to " + person.getRankName());
            } else if (rank < oldRank || (rank == oldRank && rankLevel < oldRankLevel)) {
                person.addLogEntry(getDate(), "Demoted to " + person.getRankName());
            }
        }
    }

    public GameOptions getGameOptions() {
        return gameOptions;
    }

    public Vector<IBasicOption> getGameOptionsVector() {
        Vector<IBasicOption> options = new Vector<IBasicOption>();
        for (Enumeration<IOptionGroup> i = gameOptions.getGroups(); i
                .hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements(); ) {
                IOption option = j.nextElement();
                options.add(option);
            }
        }
        return options;
    }

    public void setGameOptions(Vector<IBasicOption> options) {
        for (IBasicOption option : options) {
            gameOptions.getOption(option.getName()).setValue(option.getValue());
        }
    }

    public void addKill(Kill k) {
        kills.add(k);
        if (getCampaignOptions().getKillsForXP() > 0
                && getCampaignOptions().getKillXPAward() > 0) {
            if ((getKillsFor(k.getPilotId()).size() % getCampaignOptions()
                    .getKillsForXP()) == 0) {
                Person p = getPerson(k.getPilotId());
                if (null != p) {
                    p.setXp(p.getXp() + getCampaignOptions().getKillXPAward());
                    MekHQ.triggerEvent(new PersonChangedEvent(p));
                }
            }
        }
    }

    public ArrayList<Kill> getKillsFor(UUID pid) {
        ArrayList<Kill> personalKills = new ArrayList<Kill>();
        for (Kill k : kills) {
            if (k.getPilotId().equals(pid)) {
                personalKills.add(k);
            }
        }
        Collections.sort(personalKills, new Comparator<Kill>() {
            @Override
            public int compare(final Kill u1, final Kill u2) {
                return u1.getDate().compareTo(u2.getDate());
            }
        });
        return personalKills;
    }

    public PartsStore getPartsStore() {
        return partsStore;
    }

    public void addCustom(String name) {
        customs.add(name);
    }

    public boolean isCustom(Unit u) {
        return customs.contains(u.getEntity().getChassis() + " "
                + u.getEntity().getModel());
    }

    /**
     * borrowed from megamek.client
     */
    private void checkDuplicateNamesDuringAdd(Entity entity) {
        if (duplicateNameHash.get(entity.getShortName()) == null) {
            duplicateNameHash.put(entity.getShortName(), Integer.valueOf(1));
        } else {
            int count = duplicateNameHash.get(entity.getShortName());
            count++;
            duplicateNameHash.put(entity.getShortName(), Integer.valueOf(count));
            entity.duplicateMarker = count;
            entity.generateShortName();
            entity.generateDisplayName();
        }
    }

    /**
     * If we remove a unit, we may need to update the duplicate identifier. TODO: This function is super slow :(
     *
     * @param entity
     */
    private void checkDuplicateNamesDuringDelete(Entity entity) {
        Object o = duplicateNameHash.get(entity.getShortNameRaw());
        if (o != null) {
            int count = ((Integer) o).intValue();
            if (count > 1) {
                for (Unit u : getUnits()) {
                    Entity e = u.getEntity();
                    if (e.getShortNameRaw().equals(entity.getShortNameRaw())
                            && (e.duplicateMarker > entity.duplicateMarker)) {
                        e.duplicateMarker--;
                        e.generateShortName();
                        e.generateDisplayName();
                    }
                }
                duplicateNameHash.put(entity.getShortNameRaw(), 
                    Integer.valueOf(count - 1));
            } else {
                duplicateNameHash.remove(entity.getShortNameRaw());
            }
        }
    }

    public void hirePersonnelFor(UUID uid) {
        Unit unit = getUnit(uid);
        if (null == unit) {
            return;
        }
        while (unit.canTakeMoreDrivers()) {
            Person p = null;
            if (unit.getEntity() instanceof LandAirMech) {
                p = newPerson(Person.T_MECHWARRIOR, Person.T_AERO_PILOT);
            } else if (unit.getEntity() instanceof Mech) {
                p = newPerson(Person.T_MECHWARRIOR);
            } else if (unit.getEntity() instanceof SmallCraft
                    || unit.getEntity() instanceof Jumpship) {
                p = newPerson(Person.T_SPACE_PILOT);
            } else if (unit.getEntity() instanceof ConvFighter) {
                p = newPerson(Person.T_CONV_PILOT);
            } else if (unit.getEntity() instanceof Aero) {
                p = newPerson(Person.T_AERO_PILOT);
            } else if (unit.getEntity() instanceof Tank) {
                switch (unit.getEntity().getMovementMode()) {
                    case VTOL:
                        p = newPerson(Person.T_VTOL_PILOT);
                        break;
                    case NAVAL:
                    case HYDROFOIL:
                    case SUBMARINE:
                        p = newPerson(Person.T_NVEE_DRIVER);
                        break;
                    default:
                        p = newPerson(Person.T_GVEE_DRIVER);
                }
            } else if (unit.getEntity() instanceof Protomech) {
                p = newPerson(Person.T_PROTO_PILOT);
            } else if (unit.getEntity() instanceof BattleArmor) {
                p = newPerson(Person.T_BA);
            } else if (unit.getEntity() instanceof Infantry) {
                p = newPerson(Person.T_INFANTRY);
            }
            if (null == p) {
                break;
            }
            if (!recruitPerson(p)) {
                return;
            }
            if (unit.usesSoloPilot() || unit.usesSoldiers()) {
                unit.addPilotOrSoldier(p);
            } else {
                unit.addDriver(p);
            }
        }
        while (unit.canTakeMoreGunners()) {
            Person p = null;
            if (unit.getEntity() instanceof Tank) {
                p = newPerson(Person.T_VEE_GUNNER);
            } else if (unit.getEntity() instanceof SmallCraft
                    || unit.getEntity() instanceof Jumpship) {
                p = newPerson(Person.T_SPACE_GUNNER);
            } else if (unit.getEntity() instanceof Mech) {
                p = newPerson(Person.T_MECHWARRIOR);
            }
            if (!recruitPerson(p)) {
                return;
            }
            unit.addGunner(p);
        }
        while (unit.canTakeMoreVesselCrew()) {
            Person p = newPerson(Person.T_SPACE_CREW);
            if (!recruitPerson(p)) {
                return;
            }
            unit.addVesselCrew(p);
        }
        if (unit.canTakeNavigator()) {
            Person p = newPerson(Person.T_NAVIGATOR);
            if (!recruitPerson(p)) {
                return;
            }
            unit.setNavigator(p);
        }
        if (unit.canTakeTechOfficer()) {
            Person p = null;
            //For vehicle command console we will default to gunner
            if (unit.getEntity() instanceof Tank) {
                p = newPerson(Person.T_VEE_GUNNER);
            } else {
                p = newPerson(Person.T_MECHWARRIOR);
            }
            if (!recruitPerson(p)) {
                return;
            }
            unit.setTechOfficer(p);
        }
        unit.resetPilotAndEntity();
        unit.runDiagnostic(false);
    }

    public String getUnitRatingText() {
        return getUnitRating().getUnitRating();
    }

    /**
     * Against the Bot Calculates and returns dragoon rating if that is the chosen
     * method; for IOps method, returns unit reputation / 10. If the player chooses
     * not to use unit rating at all, use a default value of C. Note that the AtB
     * system is designed for use with FMMerc dragoon rating, and use of the IOps
     * Beta system may have unsatisfactory results, but we follow the options set by
     * the user here.
     */
    public int getUnitRatingMod() {
        if (!getCampaignOptions().useDragoonRating()) {
            return IUnitRating.DRAGOON_C;
        }
        IUnitRating rating = getUnitRating();
        return getCampaignOptions().getUnitRatingMethod()
                .equals(mekhq.campaign.rating.UnitRatingMethod.FLD_MAN_MERCS_REV) ? rating.getUnitRatingAsInteger()
                        : rating.getModifier();
    }

    public RandomSkillPreferences getRandomSkillPreferences() {
        return rskillPrefs;
    }

    public void setRandomSkillPreferences(RandomSkillPreferences prefs) {
        rskillPrefs = prefs;
    }

    public void setStartingPlanet() {
        Map<String, Planet> planetList = Planets.getInstance().getPlanets();
        Planet startingPlanet = planetList.get(getFaction().getStartingPlanet(getGameYear()));

        if (startingPlanet == null) {
            startingPlanet = planetList.get(JOptionPane.showInputDialog(
                    "This faction does not have a starting planet for this era. Please choose a planet."));
            while (startingPlanet == null) {
                startingPlanet = planetList.get(JOptionPane
                        .showInputDialog("This planet you entered does not exist. Please choose a valid planet."));
            }
        }
        location = new CurrentLocation(startingPlanet, 0);
    }

    public void addLogEntry(Person p, LogEntry entry) {
        p.addLogEntry(entry);
    }

    private ArrayList<String> getPossibleRandomPortraits (DirectoryItems portraits, ArrayList<String> existingPortraits, String subDir ) {
        ArrayList<String> possiblePortraits = new ArrayList<String>();
        Iterator<String> categories = portraits.getCategoryNames();
        while (categories.hasNext()) {
            String category = categories.next();
            if (category.endsWith(subDir)) {
                Iterator<String> names = portraits.getItemNames(category);
                while (names.hasNext()) {
                    String name = names.next();
                    String location = category + ":" + name;
                    if (existingPortraits.contains(location)) {
                        continue;
                    }
                    possiblePortraits.add(location);
                }
            }
        }
        return possiblePortraits;
    }

    public void assignRandomPortraitFor(Person p) {
        // first create a list of existing portait strings, so we can check for
        // duplicates
        ArrayList<String> existingPortraits = new ArrayList<String>();
        for (Person existingPerson : this.getPersonnel()) {
            existingPortraits.add(existingPerson.getPortraitCategory() + ":"
                    + existingPerson.getPortraitFileName());
        }
        // TODO: it would be nice to pull the portraits directory from MekHQ
        // itself
        DirectoryItems portraits;
        try {
            portraits = new DirectoryItems(
                    new File("data/images/portraits"), "", //$NON-NLS-1$ //$NON-NLS-2$
                    PortraitFileFactory.getInstance());
        } catch (Exception e) {
            return;
        }
        ArrayList<String> possiblePortraits = new ArrayList<String>();

        // Will search for portraits in the /gender/primaryrole folder first,
        // and if none are found then /gender/rolegroup, then /gender/combat or
        // /gender/support, then in /gender.
        String searchCat_Gender = "";
        if (p.getGender() == Person.G_FEMALE) {
            searchCat_Gender += "Female/";
        } else {
            searchCat_Gender += "Male/";
        }
        String searchCat_Role = Person.getRoleDesc(p.getPrimaryRole(), false) + "/";
        String searchCat_RoleGroup = "";
        String searchCat_CombatSupport = "";
        if (p.getPrimaryRole() == Person.T_ADMIN_COM
                || p.getPrimaryRole() == Person.T_ADMIN_HR
                || p.getPrimaryRole() == Person.T_ADMIN_LOG
                || p.getPrimaryRole() == Person.T_ADMIN_TRA) {
            searchCat_RoleGroup = "Admin/";
        }
        if (p.getPrimaryRole() == Person.T_MECHANIC
                || p.getPrimaryRole() == Person.T_AERO_TECH
                || p.getPrimaryRole() == Person.T_MECH_TECH
                || p.getPrimaryRole() == Person.T_BA_TECH) {
            searchCat_RoleGroup = "Tech/";
        }
        if (p.getPrimaryRole() == Person.T_MEDIC
                || p.getPrimaryRole() == Person.T_DOCTOR) {
            searchCat_RoleGroup = "Medical/";
        }
        if (p.getPrimaryRole() == Person.T_SPACE_CREW
                || p.getPrimaryRole() == Person.T_SPACE_GUNNER
                || p.getPrimaryRole() == Person.T_SPACE_PILOT
                || p.getPrimaryRole() == Person.T_NAVIGATOR) {
            searchCat_RoleGroup = "Vessel Crew/";
        }

        if (p.isSupport()) {
            searchCat_CombatSupport = "Support/";
        } else {
            searchCat_CombatSupport = "Combat/";
        }

        possiblePortraits = getPossibleRandomPortraits(portraits, existingPortraits, searchCat_Gender + searchCat_Role);

        if (possiblePortraits.isEmpty() && !searchCat_RoleGroup.isEmpty()) {
            possiblePortraits = getPossibleRandomPortraits(portraits, existingPortraits, searchCat_Gender + searchCat_RoleGroup);
        }
        if (possiblePortraits.isEmpty()) {
            possiblePortraits = getPossibleRandomPortraits(portraits, existingPortraits, searchCat_Gender + searchCat_CombatSupport);
        }
        if (possiblePortraits.isEmpty()) {
            possiblePortraits = getPossibleRandomPortraits(portraits, existingPortraits, searchCat_Gender);
        }
        if (!possiblePortraits.isEmpty()) {
            String chosenPortrait = possiblePortraits.get(Compute.randomInt(possiblePortraits.size()));
            String[] temp = chosenPortrait.split(":");
            if (temp.length != 2) {
                return;
            }
            p.setPortraitCategory(temp[0]);
            p.setPortraitFileName(temp[1]);
        }
    }

    public void clearGameData(Entity entity) {
        for (Mounted m : entity.getEquipment()) {
            m.setUsedThisRound(false);
            m.resetJam();
        }
        entity.setDeployed(false);
        entity.setElevation(0);
        entity.setPassedThrough(new Vector<Coords>());
        entity.resetFiringArcs();
        entity.resetBays();
        entity.setEvading(false);
        entity.setFacing(0);
        entity.setPosition(null);
        entity.setProne(false);
        entity.setHullDown(false);
        entity.heat = 0;
        entity.heatBuildup = 0;
        entity.setTransportId(Entity.NONE);
        entity.setUnloaded(false);
        entity.setDone(false);
        entity.resetTransporter();
        entity.setDeployRound(0);
        entity.setSwarmAttackerId(Entity.NONE);
        entity.setSwarmTargetId(Entity.NONE);
        entity.setLastTarget(Entity.NONE);
        entity.setNeverDeployed(true);
        entity.setStuck(false);
        entity.resetCoolantFailureAmount();
        entity.setConversionMode(0);
        entity.setDoomed(false);

        if (!entity.getSensors().isEmpty()) {
            entity.setNextSensor(entity.getSensors().firstElement());
        }

        if (entity instanceof IBomber) {
            IBomber bomber = (IBomber) entity;
            List<Mounted> mountedBombs = bomber.getBombs();
            if (mountedBombs.size() > 0) {
                //This should return an int[] filled with 0's
                int[] bombChoices = bomber.getBombChoices();
                for (Mounted m : mountedBombs) {
                    if (!(m.getType() instanceof BombType)) {
                        continue;
                    }
                    if(m.getBaseShotsLeft() == 1) {
                        bombChoices[BombType.getBombTypeFromInternalName(m.getType().getInternalName())] += 1;
                    }
                }
                bomber.setBombChoices(bombChoices);
                bomber.clearBombs();
            }
        }

        if (entity instanceof Mech) {
            Mech m = (Mech) entity;
            m.setCoolingFlawActive(false);
        } else if (entity instanceof Aero) {
            Aero a = (Aero) entity;

            if(a.isSpheroid()) {
                entity.setMovementMode(EntityMovementMode.SPHEROID);
            } else {
                entity.setMovementMode(EntityMovementMode.AERODYNE);
            }
            a.setAltitude(5);
            a.setCurrentVelocity(0);
            a.setNextVelocity(0);
        } else if(entity instanceof Tank) {
            Tank t = (Tank) entity;
            t.unjamTurret(t.getLocTurret());
            t.unjamTurret(t.getLocTurret2());
            t.resetJammedWeapons();
        }
        entity.getSecondaryPositions().clear();
        // TODO: still a lot of stuff to do here, but oh well
        entity.setOwner(player);
        entity.setGame(game);
    }

    public Part checkForExistingSparePart(Part part) {
        for (Part spare : getSpareParts()) {
            if (spare.getId() == part.getId()) {
                continue;
            }
            if (part.isSamePartTypeAndStatus(spare)) {
                return spare;
            }
        }
        return null;
    }

    public void refreshNetworks() {
        for (Unit unit : getUnits()) {
            // we are going to rebuild the c3 and c3i networks based on
            // the c3UUIDs
            // TODO: can we do this more efficiently?
            // this code is cribbed from megamek.server#receiveEntityAdd
            Entity entity = unit.getEntity();
            if (null != entity && (entity.hasC3() || entity.hasC3i())) {
                boolean C3iSet = false;

                for (Entity e : game.getEntitiesVector()) {
                    // C3 Checks
                    if (entity.hasC3()) {
                        if ((entity.getC3MasterIsUUIDAsString() != null)
                                && entity.getC3MasterIsUUIDAsString().equals(e.getC3UUIDAsString())) {
                            entity.setC3Master(e, false);
                            break;
                        }
                    }

                    // C3i Checks// C3i Checks
                    if (entity.hasC3i() && (C3iSet == false)) {
                        entity.setC3NetIdSelf();
                        int pos = 0;
                        while (pos < Entity.MAX_C3i_NODES) {
                            // We've found a network, join it.
                            if ((entity.getC3iNextUUIDAsString(pos) != null)
                                    && (e.getC3UUIDAsString() != null)
                                    && entity.getC3iNextUUIDAsString(pos).equals(e.getC3UUIDAsString())) {
                                entity.setC3NetId(e);
                                C3iSet = true;
                                break;
                            }

                            pos++;
                        }
                    }
                }
            }
        }
    }

    public void disbandNetworkOf(Unit u) {
        // collect all of the other units on this network to rebuild the uuids
        Vector<Unit> networkedUnits = new Vector<Unit>();
        for (Unit unit : getUnits()) {
            if (null != unit.getEntity().getC3NetId()
                    && unit.getEntity().getC3NetId().equals(u.getEntity().getC3NetId())) {
                networkedUnits.add(unit);
            }
        }
        for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
            for (Unit nUnit : networkedUnits) {
                nUnit.getEntity().setC3iNextUUIDAsString(pos, null);
            }
        }
        refreshNetworks();
        MekHQ.triggerEvent(new NetworkChangedEvent(networkedUnits));
    }

    public void removeUnitsFromNetwork(Vector<Unit> removedUnits) {
        // collect all of the other units on this network to rebuild the uuids
        Vector<String> uuids = new Vector<String>();
        Vector<Unit> networkedUnits = new Vector<Unit>();
        String network = removedUnits.get(0).getEntity().getC3NetId();
        for (Unit unit : getUnits()) {
            if (removedUnits.contains(unit)) {
                continue;
            }
            if (null != unit.getEntity().getC3NetId()
                    && unit.getEntity().getC3NetId().equals(network)) {
                networkedUnits.add(unit);
                uuids.add(unit.getEntity().getC3UUIDAsString());
            }
        }
        for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
            for (Unit u : removedUnits) {
                u.getEntity().setC3iNextUUIDAsString(pos, null);
            }
            for (Unit nUnit : networkedUnits) {
                if (pos < uuids.size()) {
                    nUnit.getEntity().setC3iNextUUIDAsString(pos,
                            uuids.get(pos));
                } else {
                    nUnit.getEntity().setC3iNextUUIDAsString(pos, null);
                }
            }
        }
        refreshNetworks();
    }

    public void addUnitsToNetwork(Vector<Unit> addedUnits, String netid) {
        // collect all of the other units on this network to rebuild the uuids
        Vector<String> uuids = new Vector<String>();
        Vector<Unit> networkedUnits = new Vector<Unit>();
        for (Unit u : addedUnits) {
            uuids.add(u.getEntity().getC3UUIDAsString());
            networkedUnits.add(u);
        }
        for (Unit unit : getUnits()) {
            if (addedUnits.contains(unit)) {
                continue;
            }
            if (null != unit.getEntity().getC3NetId()
                    && unit.getEntity().getC3NetId().equals(netid)) {
                networkedUnits.add(unit);
                uuids.add(unit.getEntity().getC3UUIDAsString());
            }
        }
        for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
            for (Unit nUnit : networkedUnits) {
                if (pos < uuids.size()) {
                    nUnit.getEntity().setC3iNextUUIDAsString(pos,
                            uuids.get(pos));
                } else {
                    nUnit.getEntity().setC3iNextUUIDAsString(pos, null);
                }
            }
        }
        refreshNetworks();
        MekHQ.triggerEvent(new NetworkChangedEvent(addedUnits));
    }

    public Vector<String[]> getAvailableC3iNetworks() {
        Vector<String[]> networks = new Vector<String[]>();
        Vector<String> networkNames = new Vector<String>();

        for(Unit u : getUnits()) {

            if (u.getForceId() < 0) {
                // only units currently in the TO&E
                continue;
            }
            Entity en = u.getEntity();
            if (null == en) {
                continue;
            }
            if (en.hasC3i() && en.calculateFreeC3Nodes() < 5
                    && en.calculateFreeC3Nodes() > 0) {
                String[] network = new String[2];
                network[0] = en.getC3NetId();
                network[1] = "" + en.calculateFreeC3Nodes();
                if (!networkNames.contains(network[0])) {
                    networks.add(network);
                    networkNames.add(network[0]);
                }
            }
        }
        return networks;
    }

    public Vector<String[]> getAvailableC3MastersForSlaves() {
        Vector<String[]> networks = new Vector<String[]>();
        Vector<String> networkNames = new Vector<String>();

        for(Unit u : getUnits()) {

            if (u.getForceId() < 0) {
                // only units currently in the TO&E
                continue;
            }
            Entity en = u.getEntity();
            if (null == en) {
                continue;
            }
            // count of free c3 nodes for single company-level masters
            // will not be right so skip
            if (en.hasC3M() && !en.hasC3MM() && en.C3MasterIs(en)) {
                continue;
            }
            if (en.calculateFreeC3Nodes() > 0) {
                String[] network = new String[3];
                network[0] = en.getC3UUIDAsString();
                network[1] = "" + en.calculateFreeC3Nodes();
                network[2] = "" + en.getShortName();
                if (!networkNames.contains(network[0])) {
                    networks.add(network);
                    networkNames.add(network[0]);
                }
            }
        }

        return networks;
    }

    public Vector<String[]> getAvailableC3MastersForMasters() {
        Vector<String[]> networks = new Vector<String[]>();
        Vector<String> networkNames = new Vector<String>();

        for(Unit u : getUnits()) {

            if (u.getForceId() < 0) {
                // only units currently in the TO&E
                continue;
            }
            Entity en = u.getEntity();
            if (null == en) {
                continue;
            }
            if (en.calculateFreeC3MNodes() > 0) {
                String[] network = new String[3];
                network[0] = en.getC3UUIDAsString();
                network[1] = "" + en.calculateFreeC3MNodes();
                network[2] = "" + en.getShortName();
                if (!networkNames.contains(network[0])) {
                    networks.add(network);
                    networkNames.add(network[0]);
                }
            }
        }

        return networks;
    }

    public void removeUnitsFromC3Master(Unit master) {
        List<Unit> removed = new ArrayList<>();
        for (Unit unit : getUnits()) {
            if (null != unit.getEntity().getC3MasterIsUUIDAsString()
                    && unit.getEntity().getC3MasterIsUUIDAsString().equals(master.getEntity().getC3UUIDAsString())) {
                unit.getEntity().setC3MasterIsUUIDAsString(null);
                unit.getEntity().setC3Master(null, true);
                removed.add(unit);
            }
        }
        refreshNetworks();
        MekHQ.triggerEvent(new NetworkChangedEvent(removed));
    }

    /**
     * This function reloads the game entities into the game at the end of scenario resolution, so that entities are
     * properly updated and destroyed ones removed
     */
    public void reloadGameEntities() {
        game.reset();
        for (Map.Entry<UUID, Unit> u : units.entrySet()) {
            Entity en = u.getValue().getEntity();
            if (null != en) {
                game.addEntity(en.getId(), en);
            }
        }
    }

    public void completeMission(int id, int status) {
        Mission mission = getMission(id);
        if (null == mission) {
            return;
        }
        mission.setStatus(status);
        if (mission instanceof Contract) {
            Contract contract = (Contract) mission;
            // check for money in escrow
            // According to FMM(r) pg 179, both failure and breach lead to no
            // further payment even though this seems stupid
            if (contract.getStatus() == Mission.S_SUCCESS
                    && contract.getMonthsLeft(getDate()) > 0) {
                DecimalFormat formatter = new DecimalFormat();
                long remainingMoney = contract.getMonthlyPayOut()
                        * contract.getMonthsLeft(getDate());
                finances.credit(remainingMoney, Transaction.C_CONTRACT,
                        "Remaining payment for " + contract.getName(), calendar.getTime());
                addReport("Your account has been credited for "
                        + formatter.format(remainingMoney) + " C-bills for the remaining payout from contract "
                        + contract.getName());
            }
        }
    }

    public int calculatePartTransitTime(int mos) {
        int nDice = getCampaignOptions().getNDiceTransitTime();
        int time = getCampaignOptions().getConstantTransitTime();
        if (nDice > 0) {
            time += Compute.d6(nDice);
        }
        // now step forward through the calendar
        GregorianCalendar arrivalDate = (GregorianCalendar) calendar.clone();
        switch (getCampaignOptions().getUnitTransitTime()) {
            case CampaignOptions.TRANSIT_UNIT_MONTH:
                arrivalDate.add(Calendar.MONTH, time);
                break;
            case CampaignOptions.TRANSIT_UNIT_WEEK:
                arrivalDate.add(Calendar.WEEK_OF_YEAR, time);
                break;
            case CampaignOptions.TRANSIT_UNIT_DAY:
            default:
                arrivalDate.add(Calendar.DAY_OF_MONTH, time);
        }

        // now adjust for MoS and minimums
        int mosBonus = getCampaignOptions().getAcquireMosBonus() * mos;
        switch (getCampaignOptions().getAcquireMosUnit()) {
            case CampaignOptions.TRANSIT_UNIT_MONTH:
                arrivalDate.add(Calendar.MONTH, -1 * mosBonus);
                break;
            case CampaignOptions.TRANSIT_UNIT_WEEK:
                arrivalDate.add(Calendar.WEEK_OF_YEAR, -1 * mosBonus);
                break;
            case CampaignOptions.TRANSIT_UNIT_DAY:
            default:
                arrivalDate.add(Calendar.DAY_OF_MONTH, -1 * mosBonus);
        }
        // now establish minimum date and if this is before
        GregorianCalendar minimumDate = (GregorianCalendar) calendar.clone();
        switch (getCampaignOptions().getAcquireMinimumTimeUnit()) {
            case CampaignOptions.TRANSIT_UNIT_MONTH:
                minimumDate.add(Calendar.MONTH, getCampaignOptions()
                        .getAcquireMinimumTime());
                break;
            case CampaignOptions.TRANSIT_UNIT_WEEK:
                minimumDate.add(Calendar.WEEK_OF_YEAR, getCampaignOptions()
                        .getAcquireMinimumTime());
                break;
            case CampaignOptions.TRANSIT_UNIT_DAY:
            default:
                minimumDate.add(Calendar.DAY_OF_MONTH, getCampaignOptions()
                        .getAcquireMinimumTime());
        }

        if (arrivalDate.before(minimumDate)) {
            return Utilities.getDaysBetween(calendar.getTime(),
                    minimumDate.getTime());
        } else {
            return Utilities.getDaysBetween(calendar.getTime(),
                    arrivalDate.getTime());
        }

    }

    /**
     * This returns a PartInventory object detailing the current count
     * for a part on hand, in transit, and ordered.
     *
     * @param part A part to lookup its current inventory.
     * @return A PartInventory object detailing the current counts of
     * the part on hand, in transit, and ordered.
     * @see mekhq.campaign.parts.PartInventory
     */
    public PartInventory getPartInventory(Part part) {
        PartInventory inventory = new PartInventory();

        int nSupply = 0;
        int nTransit = 0;
        for (Part p : getParts()) {
            if (!p.isSpare()) {
                continue;
            }
            if (part.isSamePartType(p)) {
                if (p.isPresent()) {
                    if (p instanceof Armor) {
                        nSupply += ((Armor) p).getAmount();
                    } else if (p instanceof ProtomekArmor) {
                        nSupply += ((ProtomekArmor) p).getAmount();
                    } else if (p instanceof BaArmor) {
                        nSupply += ((BaArmor) p).getAmount();
                    } else if (p instanceof AmmoStorage) {
                        nSupply += ((AmmoStorage) p).getShots();
                    } else {
                        nSupply += p.getQuantity();
                    }
                } else {
                    if (p instanceof Armor) {
                        nTransit += ((Armor) p).getAmount();
                    } else if (p instanceof ProtomekArmor) {
                        nTransit += ((ProtomekArmor) p).getAmount();
                    } else if (p instanceof BaArmor) {
                        nTransit += ((BaArmor) p).getAmount();
                    } else if (p instanceof AmmoStorage) {
                        nTransit += ((AmmoStorage) p).getShots();
                    } else {
                        nTransit += p.getQuantity();
                    }
                }
            }
        }

        inventory.setSupply(nSupply);
        inventory.setTransit(nTransit);

        int nOrdered = 0;
        IAcquisitionWork onOrder = getShoppingList().getShoppingItem(part);
        if (null != onOrder) {
            if (onOrder instanceof Armor) {
                nOrdered += ((Armor) onOrder).getAmount();
            } else if (onOrder instanceof ProtomekArmor) {
                nOrdered += ((ProtomekArmor) onOrder).getAmount();
            } else if (onOrder instanceof BaArmor) {
                nOrdered += ((BaArmor) onOrder).getAmount();
            } else if (onOrder instanceof AmmoStorage) {
                nOrdered += ((AmmoStorage) onOrder).getShots();
            } else {
                nOrdered += onOrder.getQuantity();
            }
        }

        inventory.setOrdered(nOrdered);

        String countModifier = "";
        if (part instanceof Armor || part instanceof ProtomekArmor
                || part instanceof BaArmor) {
            countModifier = "points";
        }
        if (part instanceof AmmoStorage) {
            countModifier = "shots";
        }
        
        inventory.setCountModifier(countModifier);
        return inventory;
    }

    public long getTotalEquipmentValue() {
        long value = 0;
        for (Unit u : getUnits()) {
            value += u.getSellValue();
        }
        for (Part p : getSpareParts()) {
            value += p.getActualValue();
        }
        return value;
    }

    /**
     * Calculate the total value of units in the TO&E. This serves as the basis for contract payments in the StellarOps
     * Beta.
     *
     * @return
     */
    public long getForceValue() {
        return getForceValue(false);
    }

    /**
     * Calculate the total value of units in the TO&E. This serves as the basis for contract payments in the StellarOps
     * Beta.
     *
     * @return
     */
    public long getForceValue(boolean noInfantry) {
        long value = 0;
        for (UUID uuid : forces.getAllUnits()) {
            Unit u = getUnit(uuid);
            if (null == u) {
                continue;
            }
            if (noInfantry && ((u.getEntity().getEntityType() & Entity.ETYPE_INFANTRY) == Entity.ETYPE_INFANTRY)
                    && !((u.getEntity().getEntityType() & Entity.ETYPE_BATTLEARMOR) == Entity.ETYPE_BATTLEARMOR)) {
                continue;
            }
            if (u.getEntity().hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
                if (getCampaignOptions().getDropshipContractPercent() == 0) {
                    continue;
                }
                value += getEquipmentContractValue(u, getCampaignOptions().useEquipmentContractSaleValue());
            } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_WARSHIP)) {
                if (getCampaignOptions().getWarshipContractPercent() == 0) {
                    continue;
                }
                value += getEquipmentContractValue(u, getCampaignOptions().useEquipmentContractSaleValue());
            } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_JUMPSHIP) || u.getEntity().hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
                if (getCampaignOptions().getJumpshipContractPercent() == 0) {
                    continue;
                }
                value += getEquipmentContractValue(u, getCampaignOptions().useEquipmentContractSaleValue());
            } else {
                value += getEquipmentContractValue(u, getCampaignOptions().useEquipmentContractSaleValue());
            }
        }
        return value;
    }

    public long getEquipmentContractValue(Unit u, boolean useSaleValue) {
        long value;
        long percentValue = 0;
        if (useSaleValue) {
            value = u.getSellValue();
        } else {
            value = u.getBuyCost();
        }

        if (u.getEntity().hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
            percentValue = (long) ((getCampaignOptions().getDropshipContractPercent() / 100) * value);
        } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            percentValue = (long) ((getCampaignOptions().getWarshipContractPercent() / 100) * value);
        } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_JUMPSHIP) || u.getEntity().hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            percentValue = (long) ((getCampaignOptions().getJumpshipContractPercent() / 100) * value);
        } else {
            percentValue = (long) ((getCampaignOptions().getEquipmentContractPercent() / 100) * value);
        }

        return percentValue;
    }

    public long getContractBase() {
        if (getCampaignOptions().usePeacetimeCost()) {
            double peacetimecost = (getPeacetimeCost() * .75) +
                    getForceValue(getCampaignOptions().useInfantryDontCount());
            return (long) peacetimecost;
        } else if (getCampaignOptions().useEquipmentContractBase()) {
            return getForceValue(getCampaignOptions().useInfantryDontCount());
        } else {
            return getTheoreticalPayroll(getCampaignOptions().useInfantryDontCount());
        }
    }

    public void addLoan(Loan loan) {
        addReport("You have taken out loan " + loan.getDescription()
                + ". Your account has been credited " + DecimalFormat.getInstance().format(loan.getPrincipal())
                + " for the principal amount.");
        finances.addLoan(loan);
        MekHQ.triggerEvent(new LoanNewEvent(loan));
        finances.credit(loan.getPrincipal(), Transaction.C_LOAN_PRINCIPAL,
                "loan principal for " + loan.getDescription(), calendar.getTime());

    }

    public void payOffLoan(Loan loan) {
        if (finances.debit(loan.getRemainingValue(),
                Transaction.C_LOAN_PAYMENT, "loan payoff for " + loan.getDescription(), calendar.getTime())) {
            addReport("You have paid off the remaining loan balance of "
                    + DecimalFormat.getInstance().format(loan.getRemainingValue()) + "on " + loan.getDescription());
            finances.removeLoan(loan);
            MekHQ.triggerEvent(new LoanPaidEvent(loan));
        } else {
            addReport("<font color='red'>You do not have enough funds to pay off "
                    + loan.getDescription() + "</font>");
        }

    }

    public String getFinancialReport() {
        StringBuffer sb = new StringBuffer();
        long cash = finances.getBalance();
        long loans = finances.getLoanBalance();
        long mech = 0;
        long vee = 0;
        long ba = 0;
        long infantry = 0;
        long smallCraft = 0;
        long largeCraft = 0;
        long proto = 0;
        long spareParts = 0;
        for (Map.Entry<UUID, Unit> mu : units.entrySet()) {
            Unit u = mu.getValue();
            long value = u.getSellValue();
            if (u.getEntity() instanceof Mech) {
                mech += value;
            } else if (u.getEntity() instanceof Tank) {
                vee += value;
            } else if (u.getEntity() instanceof BattleArmor) {
                ba += value;
            } else if (u.getEntity() instanceof Infantry) {
                infantry += value;
            } else if (u.getEntity() instanceof Dropship
                    || u.getEntity() instanceof Jumpship) {
                largeCraft += value;
            } else if (u.getEntity() instanceof Aero) {
                smallCraft += value;
            } else if (u.getEntity() instanceof Protomech) {
                proto += value;
            }
        }
        for (Part p : getSpareParts()) {
            spareParts += (p.getActualValue() * p.getQuantity());
        }

        long monthlyIncome = 0;
        long monthlyExpenses = 0;
        long coSpareParts = 0;
        long coFuel = 0;
        long coAmmo = 0;
        long maintenance = 0;
        long salaries = 0;
        long overhead = 0;
        long contracts = 0;
        if (campaignOptions.payForMaintain()) {
            maintenance = getWeeklyMaintenanceCosts() * 4;
        }
        if (campaignOptions.payForSalaries()) {
            salaries = getPayRoll();
        }
        if (campaignOptions.payForOverhead()) {
            overhead = getOverheadExpenses();
        }
        if (campaignOptions.usePeacetimeCost()) {
            coSpareParts = getMonthlySpareParts();
            coAmmo = getMonthlyAmmo();
            coFuel = getMonthlyFuel();
        }
        for (Contract contract : getActiveContracts()) {
            contracts += contract.getMonthlyPayOut();
        }
        monthlyIncome += contracts;
        monthlyExpenses = maintenance + salaries + overhead + coSpareParts + coAmmo + coFuel;

        long assets = cash + mech + vee + ba + infantry + largeCraft
                + smallCraft + proto + spareParts + getFinances().getTotalAssetValue();
        long liabilities = loans;
        long netWorth = assets - liabilities;
        int longest = Math.max(DecimalFormat.getInstance().format(liabilities)
                .length(), DecimalFormat.getInstance().format(assets).length());
        longest = Math.max(DecimalFormat.getInstance().format(netWorth)
                .length(), longest);
        String formatted = "%1$" + longest + "s";
        sb.append("Net Worth................ ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(netWorth))).append("\n\n");
        sb.append("    Assets............... ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(assets))).append("\n");
        sb.append("       Cash.............. ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(cash))).append("\n");
        if (mech > 0) {
            sb.append("       Mechs............. ")
                    .append(String.format(formatted, DecimalFormat.getInstance().format(mech))).append("\n");
        }
        if (vee > 0) {
            sb.append("       Vehicles.......... ")
                    .append(String.format(formatted, DecimalFormat.getInstance().format(vee))).append("\n");
        }
        if (ba > 0) {
            sb.append("       BattleArmor....... ")
                    .append(String.format(formatted, DecimalFormat.getInstance().format(ba))).append("\n");
        }
        if (infantry > 0) {
            sb.append("       Infantry.......... ")
                    .append(String.format(formatted, DecimalFormat.getInstance().format(infantry))).append("\n");
        }
        if (proto > 0) {
            sb.append("       Protomechs........ ")
                    .append(String.format(formatted, DecimalFormat.getInstance().format(proto))).append("\n");
        }
        if (smallCraft > 0) {
            sb.append("       Small Craft....... ")
                    .append(String.format(formatted, DecimalFormat.getInstance().format(smallCraft))).append("\n");
        }
        if (largeCraft > 0) {
            sb.append("       Large Craft....... ")
                    .append(String.format(formatted, DecimalFormat.getInstance().format(largeCraft))).append("\n");
        }
        sb.append("       Spare Parts....... ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(spareParts))).append("\n");

        if (getFinances().getAllAssets().size() > 0) {
            for (Asset asset : getFinances().getAllAssets()) {
                String assetName = asset.getName();
                if (assetName.length() > 18) {
                    assetName = assetName.substring(0, 17);
                } else {
                    int numPeriods = 18 - assetName.length();
                    for (int i = 0; i < numPeriods; i++) {
                        assetName += ".";
                    }
                }
                assetName += " ";
                sb.append("       ").append(assetName)
                        .append(String.format(formatted, DecimalFormat.getInstance().format(asset.getValue())))
                        .append("\n");
            }
        }
        sb.append("\n");
        sb.append("    Liabilities.......... ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(liabilities))).append("\n");
        sb.append("       Loans............. ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(loans))).append("\n\n\n");

        sb.append("Monthly Profit........... ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(monthlyIncome - monthlyExpenses)))
                .append("\n\n");
        sb.append("Monthly Income........... ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(monthlyIncome))).append("\n");
        sb.append("    Contract Payments.... ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(contracts))).append("\n\n");
        sb.append("Monthly Expenses......... ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(monthlyExpenses))).append("\n");
        sb.append("    Salaries............. ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(salaries))).append("\n");
        sb.append("    Maintenance.......... ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(maintenance))).append("\n");
        sb.append("    Overhead............. ")
                .append(String.format(formatted, DecimalFormat.getInstance().format(overhead))).append("\n");
        if (campaignOptions.usePeacetimeCost()) {
            sb.append("    Spare Parts.......... ")
                    .append(String.format(formatted, DecimalFormat.getInstance().format(coSpareParts))).append("\n");
            sb.append("    Training Munitions... ")
                    .append(String.format(formatted, DecimalFormat.getInstance().format(coAmmo))).append("\n");
            sb.append("    Fuel................. ")
                    .append(String.format(formatted, DecimalFormat.getInstance().format(coFuel))).append("\n");
        }

        return new String(sb);
    }

    public void setHealingTimeOptions(int newHeal, int newNaturalHeal) {
        // we need to check the current values and then if necessary change the
        // times for all
        // personnel, giving them credit for their current waiting time
        int currentHeal = getCampaignOptions().getHealingWaitingPeriod();
        int currentNaturalHeal = getCampaignOptions()
                .getNaturalHealingWaitingPeriod();

        getCampaignOptions().setHealingWaitingPeriod(newHeal);
        getCampaignOptions().setNaturalHealingWaitingPeriod(newNaturalHeal);

        int healDiff = newHeal - currentHeal;
        int naturalDiff = newNaturalHeal - currentNaturalHeal;

        if (healDiff != 0 || naturalDiff != 0) {
            for (Person p : getPersonnel()) {
                if (p.getDoctorId() != null) {
                    p.setDaysToWaitForHealing(Math.max(
                            p.getDaysToWaitForHealing() + healDiff, 1));
                } else {
                    p.setDaysToWaitForHealing(Math.max(
                            p.getDaysToWaitForHealing() + naturalDiff, 1));
                }
            }
        }
    }

    public int getTotalMechBays() {
        int bays = 0;
        for (Unit u : getUnits()) {
            bays += u.getMechCapacity();
        }
        return bays;
    }

    public int getTotalASFBays() {
        int bays = 0;
        for (Unit u : getUnits()) {
            bays += u.getASFCapacity();
        }
        return bays;
    }

    public int getTotalSmallCraftBays() {
        int bays = 0;
        for (Unit u : getUnits()) {
            bays += u.getSmallCraftCapacity();
        }
        return bays;
    }

    public int getTotalBattleArmorBays() {
        int bays = 0;
        for (Unit u : getUnits()) {
            bays += u.getBattleArmorCapacity();
        }
        return bays;
    }

    public int getTotalInfantryBays() {
        int bays = 0;
        for (Unit u : getUnits()) {
            bays += u.getInfantryCapacity();
        }
        return bays;
    }

    public int getTotalHeavyVehicleBays() {
        int bays = 0;
        for (Unit u : getUnits()) {
            bays += u.getHeavyVehicleCapacity();
        }
        return bays;
    }

    public int getTotalLightVehicleBays() {
        int bays = 0;
        for (Unit u : getUnits()) {
            bays += u.getLightVehicleCapacity();
        }
        return bays;
    }

    public int getTotalProtomechBays() {
        int bays = 0;
        for (Unit u : getUnits()) {
            bays += u.getProtomechCapacity();
        }
        return bays;
    }

    public int getTotalDockingCollars() {
        int collars = 0;
        for (Unit u : getUnits()) {
            if (u.getEntity() instanceof Jumpship) {
                collars += u.getDocks();
            }
        }
        return collars;
    }

    public double getTotalInsulatedCargoCapacity() {
        double capacity = 0;
        for (Unit u : getUnits()) {
            capacity += u.getInsulatedCargoCapacity();
        }
        return capacity;
    }

    public double getTotalRefrigeratedCargoCapacity() {
        double capacity = 0;
        for (Unit u : getUnits()) {
            capacity += u.getRefrigeratedCargoCapacity();
        }
        return capacity;
    }

    public double getTotalLivestockCargoCapacity() {
        double capacity = 0;
        for (Unit u : getUnits()) {
            capacity += u.getLivestockCargoCapacity();
        }
        return capacity;
    }

    public double getTotalLiquidCargoCapacity() {
        double capacity = 0;
        for (Unit u : getUnits()) {
            capacity += u.getLiquidCargoCapacity();
        }
        return capacity;
    }

    public double getTotalCargoCapacity() {
        double capacity = 0;
        for (Unit u : getUnits()) {
            capacity += u.getCargoCapacity();
        }
        return capacity;
    }

    // Liquid not included
    public double getTotalCombinedCargoCapacity() {
        return getTotalCargoCapacity() + getTotalLivestockCargoCapacity()
                + getTotalInsulatedCargoCapacity() + getTotalRefrigeratedCargoCapacity();
    }

    public int getNumberOfUnitsByType(long type) {
        return getNumberOfUnitsByType(type, false, false);
    }

    public int getNumberOfUnitsByType(long type, boolean inTransit) {
        return getNumberOfUnitsByType(type, inTransit, false);
    }

    public int getNumberOfUnitsByType(long type, boolean inTransit, boolean lv) {
        int num = 0;
        for (Unit unit : getUnits()) {
            if (!inTransit && !unit.isPresent()) {
                continue;
            }
            if (unit.isMothballed()) {
                if (type == Unit.ETYPE_MOTHBALLED) {
                    num++;
                }
                continue;
            }
            Entity en = unit.getEntity();
            if (en instanceof GunEmplacement || en instanceof FighterSquadron || en instanceof Jumpship) {
                continue;
            }
            if (type == Entity.ETYPE_MECH && en instanceof Mech) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_DROPSHIP && en instanceof Dropship) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_SMALL_CRAFT && en instanceof SmallCraft
                    && !(en instanceof Dropship)) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_CONV_FIGHTER && en instanceof ConvFighter) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_AERO && en instanceof Aero
                    && !(en instanceof SmallCraft || en instanceof ConvFighter)) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_INFANTRY && en instanceof Infantry && !(en instanceof BattleArmor)) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_BATTLEARMOR && en instanceof BattleArmor) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_TANK && en instanceof Tank) {
                if ((en.getWeight() <= 50 && lv) || (en.getWeight() > 50 && !lv)) {
                    num++;
                }
                continue;
            }
            if (type == Entity.ETYPE_PROTOMECH && en instanceof Protomech) {
                num++;
                continue;
            }
        }

        return num;
    }

    public double getCargoTonnage(boolean inTransit) {
        return getCargoTonnage(inTransit, false);
    }

    @SuppressWarnings("unused") // FIXME: This whole method needs re-worked once Dropship Assignments are in
    public double getCargoTonnage(boolean inTransit, boolean mothballed) {
        double cargoTonnage = 0;
        double mothballedTonnage = 0;
        int mechs = getNumberOfUnitsByType(Entity.ETYPE_MECH);
        int ds = getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
        int sc = getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT);
        int cf = getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER);
        int asf = getNumberOfUnitsByType(Entity.ETYPE_AERO);
        int inf = getNumberOfUnitsByType(Entity.ETYPE_INFANTRY);
        int ba = getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR);
        int lv = getNumberOfUnitsByType(Entity.ETYPE_TANK, true);
        int hv = getNumberOfUnitsByType(Entity.ETYPE_TANK, false);
        int protos = getNumberOfUnitsByType(Entity.ETYPE_PROTOMECH);

        for (Part part : getSpareParts()) {
            if (!inTransit && !part.isPresent()) {
                continue;
            }
            cargoTonnage += (part.getQuantity() * part.getTonnage());
        }

        // place units in bays
        // FIXME: This has been temporarily disabled. It really needs dropship assignments done to fix it correctly.
        // Remaining units go into cargo
        for (Unit unit : getUnits()) {
            if (!inTransit && !unit.isPresent()) {
                continue;
            }
            Entity en = unit.getEntity();
            if (unit.isMothballed()) {
                mothballedTonnage += en.getWeight();
                continue;
            }
            if (en instanceof GunEmplacement || en instanceof FighterSquadron || en instanceof Jumpship) {
                continue;
            }
            // cargoTonnage += en.getWeight();
        }
        if (mothballed) {
            return mothballedTonnage;
        }
        return cargoTonnage;
    }

    public String getCargoDetails() {
        StringBuffer sb = new StringBuffer("Cargo\n\n");
        double ccc = this.getTotalCombinedCargoCapacity();
        double gcc = this.getTotalCargoCapacity();
        double icc = this.getTotalInsulatedCargoCapacity();
        double lcc = this.getTotalLiquidCargoCapacity();
        double scc = this.getTotalLivestockCargoCapacity();
        double rcc = this.getTotalRefrigeratedCargoCapacity();
        double tonnage = this.getCargoTonnage(false);
        double mothballedTonnage = this.getCargoTonnage(false, true);
        double mothballedUnits = Math.max(getNumberOfUnitsByType(Unit.ETYPE_MOTHBALLED), 0);
        double combined = (tonnage + mothballedTonnage);
        double transported = combined > ccc ? ccc : combined;
        double overage = combined - transported;

        sb.append(String.format("%-35s      %6.3f\n", "Total Capacity:", ccc));
        sb.append(String.format("%-35s      %6.3f\n", "General Capacity:", gcc));
        sb.append(String.format("%-35s      %6.3f\n", "Insulated Capacity:", icc));
        sb.append(String.format("%-35s      %6.3f\n", "Liquid Capacity:", lcc));
        sb.append(String.format("%-35s      %6.3f\n", "Livestock Capacity:", scc));
        sb.append(String.format("%-35s      %6.3f\n", "Refrigerated Capacity:", rcc));
        sb.append(String.format("%-35s      %6.3f\n", "Cargo Transported:", tonnage));
        sb.append(String.format("%-35s      %4s (%1.0f)\n", "Mothballed Units as Cargo (Tons):", mothballedUnits, mothballedTonnage));
        sb.append(String.format("%-35s      %6.3f/%1.3f\n", "Transported/Capacity:", transported, ccc));
        sb.append(String.format("%-35s      %6.3f\n", "Overage Not Transported:", overage));

        return new String(sb);
    }

    public int getOccupiedBays(long type) {
        return getOccupiedBays(type, false);
    }

    public int getOccupiedBays(long type, boolean lv) {
        int num = 0;
        for (Unit unit : getUnits()) {
            if (unit.isMothballed()) {
                continue;
            }
            Entity en = unit.getEntity();
            if (en instanceof GunEmplacement || en instanceof Jumpship) {
                continue;
            }
            if (type == Entity.ETYPE_MECH && en instanceof Mech) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_DROPSHIP && en instanceof Dropship) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_SMALL_CRAFT && en instanceof SmallCraft && !(en instanceof Dropship)) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_CONV_FIGHTER && en instanceof ConvFighter) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_AERO && en instanceof Aero
                    && !(en instanceof SmallCraft || en instanceof ConvFighter)) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_INFANTRY && en instanceof Infantry && !(en instanceof BattleArmor)) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_BATTLEARMOR && en instanceof BattleArmor) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_TANK && en instanceof Tank) {
                if ((en.getWeight() <= 50 && lv) || (en.getWeight() > 50 && !lv)) {
                    num++;
                }
                continue;
            }
            if (type == Entity.ETYPE_PROTOMECH && en instanceof Protomech) {
                num++;
                continue;
            }
        }

        if (type == Entity.ETYPE_MECH) {
            if (getTotalMechBays() > num) {
                return num;
            }
            return getTotalMechBays();
        }

        if (type == Entity.ETYPE_AERO) {
            if (getTotalASFBays() > num) {
                return num;
            }
            return getTotalASFBays();
        }

        if (type == Entity.ETYPE_INFANTRY) {
            if (getTotalInfantryBays() > num) {
                return num;
            }
            return getTotalInfantryBays();
        }

        if (type == Entity.ETYPE_BATTLEARMOR) {
            if (getTotalBattleArmorBays() > num) {
                return num;
            }
            return getTotalBattleArmorBays();
        }

        if (type == Entity.ETYPE_TANK) {
            if (lv) {
                if (getTotalLightVehicleBays() > num) {
                    return num;
                }
                return getTotalLightVehicleBays();
            }
            if (getTotalHeavyVehicleBays() > num) {
                return num;
            }
            return getTotalHeavyVehicleBays();
        }

        if (type == Entity.ETYPE_SMALL_CRAFT) {
            if (getTotalSmallCraftBays() > num) {
                return num;
            }
            return getTotalSmallCraftBays();
        }

        if (type == Entity.ETYPE_PROTOMECH) {
            if (getTotalProtomechBays() > num) {
                return num;
            }
            return getTotalProtomechBays();
        }

        if (type == Entity.ETYPE_DROPSHIP) {
            if (getTotalDockingCollars() > num) {
                return num;
            }
            return getTotalDockingCollars();
        }

        return -1; // default, this is an error condition
    }

    public String getTransportDetails() {
        int noMech = Math.max(getNumberOfUnitsByType(Entity.ETYPE_MECH) - getOccupiedBays(Entity.ETYPE_MECH), 0);
        int noDS = Math.max(getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP) - getOccupiedBays(Entity.ETYPE_DROPSHIP), 0);
        int noSC = Math.max(getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT) - getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        @SuppressWarnings("unused") // FIXME: What type of bays do ConvFighters use?
        int noCF = Math
                .max(getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER) - getOccupiedBays(Entity.ETYPE_CONV_FIGHTER), 0);
        int noASF = Math.max(getNumberOfUnitsByType(Entity.ETYPE_AERO) - getOccupiedBays(Entity.ETYPE_AERO), 0);
        int nolv = Math.max(getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true) - getOccupiedBays(Entity.ETYPE_TANK, true), 0);
        int nohv = Math.max(getNumberOfUnitsByType(Entity.ETYPE_TANK) - getOccupiedBays(Entity.ETYPE_TANK), 0);
        int noinf = Math.max(getNumberOfUnitsByType(Entity.ETYPE_INFANTRY) - getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int noBA = Math.max(getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR) - getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        @SuppressWarnings("unused") // FIXME: This should be used somewhere...
        int noProto = Math.max(getNumberOfUnitsByType(Entity.ETYPE_PROTOMECH) - getOccupiedBays(Entity.ETYPE_PROTOMECH),
                0);
        int freehv = Math.max(getTotalHeavyVehicleBays() - getOccupiedBays(Entity.ETYPE_TANK), 0);
        int freeinf = Math.max(getTotalInfantryBays() - getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int freeba = Math.max(getTotalBattleArmorBays() - getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        int freeSC = Math.max(getTotalSmallCraftBays() - getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        int mothballedAsCargo = Math.max(getNumberOfUnitsByType(Unit.ETYPE_MOTHBALLED), 0);

        String asfAppend = "";
        int newNoASF = Math.max(noASF - freeSC, 0);
        int placedASF = Math.max(noASF - newNoASF, 0);
        if (noASF > 0 && freeSC > 0) {
            asfAppend = " [" + placedASF + " ASF will be placed in Small Craft bays]";
            freeSC -= placedASF;
        }

        String lvAppend = "";
        int newNolv = Math.max(nolv - freehv, 0);
        int placedlv = Math.max(nolv - newNolv, 0);
        if (nolv > 0 && freehv > 0) {
            lvAppend = " [" + placedlv + " Light Vehicles will be placed in Heavy Vehicle bays]";
            freehv -= placedlv;
        }

        if (noBA > 0 && freeinf > 0) {

        }

        if (noinf > 0 && freeba > 0) {

        }

        StringBuffer sb = new StringBuffer("Transports\n\n");

        // Lets do Mechs first.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Mech Bays (Occupied):",
                getTotalMechBays(), getOccupiedBays(Entity.ETYPE_MECH), "Mechs Not Transported:", noMech));

        // Lets do ASF next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d%s\n", "ASF Bays (Occupied):",
                getTotalASFBays(), getOccupiedBays(Entity.ETYPE_AERO), "ASF Not Transported:", noASF, asfAppend));

        // Lets do Light Vehicles next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d%s\n", "Light Vehicle Bays (Occupied):",
                getTotalLightVehicleBays(), getOccupiedBays(Entity.ETYPE_TANK, true), "Light Vehicles Not Transported:",
                nolv, lvAppend));

        // Lets do Heavy Vehicles next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Heavy Vehicle Bays (Occupied):",
                getTotalHeavyVehicleBays(), getOccupiedBays(Entity.ETYPE_TANK), "Heavy Vehicles Not Transported:",
                nohv));

        if (noASF > 0 && freeSC > 0) {
            // Lets do ASF in Free Small Craft Bays next.
            sb.append(String.format("%-35s   %4d (%4d)      %-35s     %4d\n", "   Light Vehicles in Heavy Vehicle Bays (Occupied):",
                    getTotalHeavyVehicleBays(), getOccupiedBays(Entity.ETYPE_TANK) + placedlv,
                    "Light Vehicles Not Transported:", newNolv));
        }

        if (nolv > 0 && freehv > 0) {

        }

        // Lets do Infantry next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Infantry Bays (Occupied):",
                getTotalInfantryBays(), getOccupiedBays(Entity.ETYPE_INFANTRY), "Infantry Not Transported:", noinf));

        if (noBA > 0 && freeinf > 0) {

        }

        // Lets do Battle Armor next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Battle Armor Bays (Occupied):",
                getTotalBattleArmorBays(), getOccupiedBays(Entity.ETYPE_BATTLEARMOR), "Battle Armor Not Transported:",
                noBA));

        if (noinf > 0 && freeba > 0) {

        }

        // Lets do Small Craft next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Small Craft Bays (Occupied):",
                getTotalSmallCraftBays(), getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), "Small Craft Not Transported:",
                noSC));

        if (noASF > 0 && freeSC > 0) {
            // Lets do ASF in Free Small Craft Bays next.
            sb.append(String.format("%-35s   %4d (%4d)      %-35s     %4d\n", "   ASF in Small Craft Bays (Occupied):",
                    getTotalSmallCraftBays(), getOccupiedBays(Entity.ETYPE_SMALL_CRAFT) + placedASF,
                    "ASF Not Transported:", newNoASF));
        }

        // Lets do Protomechs next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Protomech Bays (Occupied):",
                getTotalProtomechBays(), getOccupiedBays(Entity.ETYPE_PROTOMECH), "Protomechs Not Transported:", noSC));

        sb.append("\n\n");

        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Docking Collars (Occupied):",
                getTotalDockingCollars(), getOccupiedBays(Entity.ETYPE_DROPSHIP), "Dropships Not Transported:", noDS));

        sb.append("\n\n");

        sb.append(String.format("%-35s      %4d\n", "Mothballed Units (see Cargo report)", mothballedAsCargo));

        return new String(sb);
    }

    public String getCombatPersonnelDetails() {
        int[] countPersonByType = new int[Person.T_NUM];
        int countTotal = 0;
        int countInjured = 0;
        int countMIA = 0;
        int countKIA = 0;
        int countRetired = 0;
        long salary = 0;

        for (Person p : getPersonnel()) {
            // Add them to the total count
            if (Person.isCombatRole(p.getPrimaryRole()) && !p.isPrisoner()
                    && !p.isBondsman() && p.isActive()) {
                countPersonByType[p.getPrimaryRole()]++;
                countTotal++;
                if (getCampaignOptions().useAdvancedMedical()
                        && p.getInjuries().size() > 0) {
                    countInjured++;
                } else if (p.getHits() > 0) {
                    countInjured++;
                }
                salary += p.getSalary();
            } else if (Person.isCombatRole(p.getPrimaryRole())
                    && p.getStatus() == Person.S_RETIRED) {
                countRetired++;
            } else if (Person.isCombatRole(p.getPrimaryRole())
                    && p.getStatus() == Person.S_MIA) {
                countMIA++;
            } else if (Person.isCombatRole(p.getPrimaryRole())
                    && p.getStatus() == Person.S_KIA) {
                countKIA++;
            }
        }

        StringBuffer sb = new StringBuffer("Combat Personnel\n\n");

        String buffer = "";

        buffer = String.format("%-30s        %4s\n", "Total Combat Personnel",
                countTotal);
        sb.append(buffer);

        for (int i = 0; i <= Person.T_NUM; i++) {
            if (Person.isCombatRole(i)) {
                buffer = String.format("    %-30s    %4s\n", Person.getRoleDesc(i, getFaction().isClan()),
                        countPersonByType[i]);
                sb.append(buffer);
            }
        }

        buffer = String.format("%-30s        %4s\n",
                "Injured Combat Personnel", countInjured);
        sb.append("\n" + buffer);
        buffer = String.format("%-30s        %4s\n", "MIA Combat Personnel",
                countMIA);
        sb.append(buffer);
        buffer = String.format("%-30s        %4s\n", "KIA Combat Personnel",
                countKIA);
        sb.append(buffer);
        buffer = String.format("%-30s        %4s\n",
                "Retired Combat Personnel", countRetired);
        sb.append(buffer);

        sb.append("\nMonthly Salary For Combat Personnel: " + salary);

        return new String(sb);
    }

    public String getSupportPersonnelDetails() {
        int[] countPersonByType = new int[Person.T_NUM];
        int countTotal = 0;
        int countInjured = 0;
        int countMIA = 0;
        int countKIA = 0;
        int countRetired = 0;
        long salary = 0;
        int prisoners = 0;
        int bondsmen = 0;

        for (Person p : getPersonnel()) {
            // Add them to the total count
            if (Person.isSupportRole(p.getPrimaryRole()) && !p.isPrisoner()
                    && !p.isBondsman() && p.isActive()) {
                countPersonByType[p.getPrimaryRole()]++;
                countTotal++;
                if (p.getInjuries().size() > 0 || p.getHits() > 0) {
                    countInjured++;
                }
                salary += p.getSalary();
            } else if (p.isPrisoner() && p.isActive()) {
                prisoners++;
                if (p.getInjuries().size() > 0 || p.getHits() > 0) {
                    countInjured++;
                }
            } else if (p.isBondsman() && p.isActive()) {
                bondsmen++;
                if (p.getInjuries().size() > 0 || p.getHits() > 0) {
                    countInjured++;
                }
            } else if (Person.isSupportRole(p.getPrimaryRole())
                    && p.getStatus() == Person.S_RETIRED) {
                countRetired++;
            } else if (Person.isSupportRole(p.getPrimaryRole())
                    && p.getStatus() == Person.S_MIA) {
                countMIA++;
            } else if (Person.isSupportRole(p.getPrimaryRole())
                    && p.getStatus() == Person.S_KIA) {
                countKIA++;
            }
        }

        StringBuffer sb = new StringBuffer("Support Personnel\n\n");

        String buffer = "";

        buffer = String.format("%-30s        %4s\n", "Total Support Personnel",
                countTotal);
        sb.append(buffer);

        for (int i = 0; i < Person.T_NUM; i++) {
            if (Person.isSupportRole(i)) {
                buffer = String.format("    %-30s    %4s\n", Person.getRoleDesc(i, getFaction().isClan()),
                        countPersonByType[i]);
                sb.append(buffer);
            }
        }

        buffer = String.format("%-30s        %4s\n",
                "Injured Support Personnel", countInjured);
        sb.append("\n" + buffer);
        buffer = String.format("%-30s        %4s\n", "MIA Support Personnel",
                countMIA);
        sb.append(buffer);
        buffer = String.format("%-30s        %4s\n", "KIA Support Personnel",
                countKIA);
        sb.append(buffer);
        buffer = String.format("%-30s        %4s\n",
                "Retired Support Personnel", countRetired);
        sb.append(buffer);

        sb.append("\nMonthly Salary For Support Personnel: " + salary);

        sb.append(String.format("\nYou have " + prisoners + " prisoner%s",
                prisoners == 1 ? "" : "s"));
        sb.append(String.format("\nYou have " + bondsmen + " %s",
                bondsmen == 1 ? "bondsman" : "bondsmen"));

        return new String(sb);
    }

    public void doMaintenance(Unit u) {
        if (!u.requiresMaintenance()) {
            return;
        }
        // lets start by checking times
        Person tech = u.getTech();
        int minutesUsed = u.getMaintenanceTime();
        int astechsUsed = getAvailableAstechs(minutesUsed, false);
        boolean maintained = null != tech
                && tech.getMinutesLeft() >= minutesUsed && !tech.isMothballing();
        boolean paidMaintenance = true;
        if (maintained) {
            // use the time
            tech.setMinutesLeft(tech.getMinutesLeft() - minutesUsed);
            astechPoolMinutes -= astechsUsed * minutesUsed;
        }
        u.incrementDaysSinceMaintenance(maintained, astechsUsed);
        if (u.getDaysSinceMaintenance() >= getCampaignOptions().getMaintenanceCycleDays()) {
            // maybe use the money
            if (campaignOptions.payForMaintain()) {
                if (finances.debit(u.getMaintenanceCost(), Transaction.C_MAINTAIN, "Maintenance for " + u.getName(),
                        calendar.getTime())) {
                } else {
                    addReport("<font color='red'><b>You cannot afford to pay maintenance costs for "
                            + u.getHyperlinkedName() + "!</b></font>");
                    paidMaintenance = false;
                }
            }
            if (getCampaignOptions().checkMaintenance()) {
                // its time for a maintenance check
                int qualityOrig = u.getQuality();
                String techName = "Nobody";
                String techNameLinked = techName;
                if (null != tech) {
                    techName = tech.getFullTitle();
                    techNameLinked = tech.getHyperlinkedFullTitle();
                }
                // dont do actual damage until we clear the for loop to avoid
                // concurrent mod problems
                // put it into a hash - 4 points of damage will mean destruction
                HashMap<Integer, Integer> partsToDamage = new HashMap<Integer, Integer>();
                String maintenanceReport = "<emph>" + techName + " performing maintenance</emph><br><br>";
                for (Part p : u.getParts()) {
                    String partReport = "<b>" + p.getName() + "</b> (Quality " + p.getQualityName() + ")";
                    if (!p.needsMaintenance()) {
                        continue;
                    }
                    int oldQuality = p.getQuality();
                    TargetRoll target = getTargetForMaintenance(p, tech);
                    if (!paidMaintenance) {
                        // I should probably make this modifier user inputtable
                        target.addModifier(1, "did not pay maintenance");
                    }
                    partReport += ", TN " + target.getValue() + "[" + target.getDesc() + "]";
                    int roll = Compute.d6(2);
                    int margin = roll - target.getValue();
                    partReport += " rolled a " + roll + ", margin of " + margin;
                    switch (p.getQuality()) {
                        case Part.QUALITY_F:
                            if (margin < -2) {
                                p.decreaseQuality();
                                if (margin < -6 && !campaignOptions.useUnofficalMaintenance()) {
                                    partsToDamage.put(p.getId(), 1);
                                }
                            }
                            if (margin >= 6) {
                                // TODO: award XP point (make this optional)
                            }
                            break;
                        case Part.QUALITY_E:
                            if (margin < -2) {
                                p.decreaseQuality();
                                if (margin < -5 && !campaignOptions.useUnofficalMaintenance()) {
                                    partsToDamage.put(p.getId(), 1);
                                }
                            }
                            if (margin >= 6) {
                                p.improveQuality();
                            }
                            break;
                        case Part.QUALITY_D:
                            if (margin < -3) {
                                p.decreaseQuality();
                                if (margin < -4 && !campaignOptions.useUnofficalMaintenance()) {
                                    partsToDamage.put(p.getId(), 1);
                                }
                            }
                            if (margin >= 5) {
                                p.improveQuality();
                            }
                            break;
                        case Part.QUALITY_C:
                            if (margin < -4) {
                                p.decreaseQuality();
                            }
                            if (!campaignOptions.useUnofficalMaintenance()) {
                                if (margin < -6) {
                                    partsToDamage.put(p.getId(), 2);
                                } else if (margin < -3) {
                                    partsToDamage.put(p.getId(), 1);
                                }
                            }
                            if (margin >= 5) {
                                p.improveQuality();
                            }
                            break;
                        case Part.QUALITY_B:
                            if (margin < -5) {
                                p.decreaseQuality();
                            }
                            if (!campaignOptions.useUnofficalMaintenance()) {
                                if (margin < -6) {
                                    partsToDamage.put(p.getId(), 2);
                                } else if (margin < -2) {
                                    partsToDamage.put(p.getId(), 1);
                                }
                            }
                            if (margin >= 4) {
                                p.improveQuality();
                            }
                            break;
                        case Part.QUALITY_A:
                            if (!campaignOptions.useUnofficalMaintenance()) {
                                if (margin < -6) {
                                    partsToDamage.put(p.getId(), 4);
                                } else if (margin < -4) {
                                    partsToDamage.put(p.getId(), 3);
                                } else if (margin == -4) {
                                    partsToDamage.put(p.getId(), 2);
                                } else if (margin < -1) {
                                    partsToDamage.put(p.getId(), 1);
                                }
                            } else if (margin < -6) {
                                partsToDamage.put(p.getId(), 1);
                            }
                            if (margin >= 4) {
                                p.improveQuality();
                            }
                            break;
                    }
                    if (p.getQuality() > oldQuality) {
                        partReport += ": <font color='green'>new quality is " + p.getQualityName() + "</font>";
                    } else if (p.getQuality() < oldQuality) {
                        partReport += ": <font color='red'>new quality is " + p.getQualityName() + "</font>";
                    } else {
                        partReport += ": quality remains " + p.getQualityName();
                    }
                    if (null != partsToDamage.get(p.getId())) {
                        if (partsToDamage.get(p.getId()) > 3) {
                            partReport += ", <font color='red'><b>part destroyed</b></font>";
                        } else {
                            partReport += ", <font color='red'><b>part damaged</b></font>";
                        }
                    }
                    maintenanceReport += partReport + "<br>";
                }
                int nDamage = 0;
                int nDestroy = 0;
                for (int key : partsToDamage.keySet()) {
                    Part p = getPart(key);
                    if (null != p) {
                        int damage = partsToDamage.get(key);
                        if (damage > 3) {
                            nDestroy++;
                            p.remove(false);
                        } else {
                            p.doMaintenanceDamage(damage);
                            nDamage++;
                        }
                    }
                }
                u.setLastMaintenanceReport(maintenanceReport);
                int quality = u.getQuality();
                String qualityString = "";
                boolean reverse = getCampaignOptions().reverseQualityNames();
                if (quality > qualityOrig) {
                    qualityString = "<font color='green'>Overall quality improves from "
                            + Part.getQualityName(qualityOrig, reverse) + " to " + Part.getQualityName(quality, reverse)
                            + "</font>";
                } else if (quality < qualityOrig) {
                    qualityString = "<font color='red'>Overall quality declines from "
                            + Part.getQualityName(qualityOrig, reverse) + " to " + Part.getQualityName(quality, reverse)
                            + "</font>";
                } else {
                    qualityString = "Overall quality remains " + Part.getQualityName(quality, reverse);
                }
                String damageString = "";
                if (nDamage > 0) {
                    damageString += nDamage + " parts were damaged. ";
                }
                if (nDestroy > 0) {
                    damageString += nDestroy + " parts were destroyed.";
                }
                if (!damageString.isEmpty()) {
                    damageString = "<b><font color='red'>" + damageString + "</b></font> [<a href='REPAIR|" + u.getId()
                            + "'>Repair bay</a>]";
                }
                String paidString = "";
                if (!paidMaintenance) {
                    paidString = "<font color='red'>Could not afford maintenance costs, so check is at a penalty.</font>";
                }
                addReport(techNameLinked + " performs maintenance on " + u.getHyperlinkedName() + ". " + paidString
                        + qualityString + ". " + damageString + " [<a href='MAINTENANCE|" + u.getId()
                        + "'>Get details</a>]");
            }
            u.resetDaysSinceMaintenance();
                }
    }

    public void initTimeInService() {
        for (Person p : getPersonnel()) {
            Date join = null;
            for (LogEntry e : p.getPersonnelLog()) {
                if (join == null){
                    // If by some nightmare there is no Joined date just use the first entry.
                    join = e.getDate();
                }
                if (e.getDesc().startsWith("Joined ") || e.getDesc().startsWith("Freed ")) {
                    join = e.getDate();
                    break;
                }
            }
            if (!p.isDependent() && !p.isPrisoner() && !p.isBondsman()) {
                GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
                // For that one in a billion chance the log is empty. Clone todays date and subtract a year
                if (join == null) {
                    cal = (GregorianCalendar)calendar.clone();
                    cal.add(Calendar.YEAR, -1);
                    p.setRecruitment(cal);
                } else {
                    cal.setTime(join);
                    p.setRecruitment(cal);
                }
            }
        }
    }

    public void initAtB() {
        retirementDefectionTracker.setLastRetirementRoll(calendar);

        /*
         * Switch all contracts to AtBContract's
         */
        for (Map.Entry<Integer, Mission> me : missions.entrySet()) {
            Mission m = me.getValue();
            if (m instanceof Contract && !(m instanceof AtBContract)) {
                me.setValue(new AtBContract((Contract)m, this));
            }
        }

        /*
         * Go through all the personnel records and assume the earliest date is the date
         * the unit was founded.
         */
        Date founding = null;
        for (Person p : getPersonnel()) {
            for (LogEntry e : p.getPersonnelLog()) {
                if (null == founding || e.getDate().before(founding)) {
                    founding = e.getDate();
                }
            }
        }
        /*
         * Go through the personnel records again and assume that any person who joined
         * the unit on the founding date is one of the founding members. Also assume
         * that MWs assigned to a non-Assault 'Mech on the date they joined came with
         * that 'Mech (which is a less certain assumption)
         */
        for (Person p : getPersonnel()) {
            Date join = null;
            for (LogEntry e : p.getPersonnelLog()) {
                if (e.getDesc().startsWith("Joined ")) {
                    join = e.getDate();
                    break;
                }
            }
            if (null != join && join.equals(founding)) {
                p.setFounder(true);
            }
            if (p.getPrimaryRole() == Person.T_MECHWARRIOR
                    || (p.getPrimaryRole() == Person.T_AERO_PILOT && getCampaignOptions().getAeroRecruitsHaveUnits())
                    || p.getPrimaryRole() == Person.T_PROTO_PILOT) {
                for (LogEntry e : p.getPersonnelLog()) {
                    if (e.getDate().equals(join) && e.getDesc().startsWith("Assigned to ")) {
                        String mech = e.getDesc().substring(12);
                        MechSummary ms = MechSummaryCache.getInstance().getMech(mech);
                        if (null != ms && (p.isFounder()
                                || ms.getWeightClass() < megamek.common.EntityWeightClass.WEIGHT_ASSAULT)) {
                            p.setOriginalUnitWeight(ms.getWeightClass());
                            if (ms.isClan()) {
                                p.setOriginalUnitTech(2);
                            } else if (ms.getYear() > 3050) {
                                /*
                                 * We're only guessing anyway, so we use this hack to avoid actually loading the
                                 * entity to check for IS2
                                 */
                                p.setOriginalUnitTech(1);
                            }
                            if (null != p.getUnitId() && null != units.get(p.getUnitId())
                                    && ms.getName().equals(units.get(p.getUnitId()).getEntity().getShortNameRaw())) {
                                p.setOriginalUnitId(p.getUnitId());
                            }
                        }
                    }
                }
            }
        }
        addAllLances(this.forces);
        atbConfig = AtBConfiguration.loadFromXml();
        RandomFactionGenerator.getInstance().startup(this);
        atbEventProcessor = new AtBEventProcessor(this);
    }
    
    /**
     * Stop processing AtB events and release memory.
     */
    public void shutdownAtB() {
        RandomFactionGenerator.getInstance().dispose();
        RandomUnitGenerator.getInstance().dispose();
        RandomNameGenerator.getInstance().dispose();
        atbEventProcessor.shutdown();
    }

    public boolean checkOverDueLoans() {
        long overdueAmount = getFinances().checkOverdueLoanPayments(this);
        if (overdueAmount > 0) {
            JOptionPane.showMessageDialog(
                    null,
                    "You have overdue loan payments totaling " + DecimalFormat.getInstance().format(overdueAmount)
                            + " C-bills.\nYou must deal with these payments before advancing the day.\nHere are some options:\n  - Sell off equipment to generate funds.\n  - Pay off the collateral on the loan.\n  - Default on the loan.\n  - Just cheat and remove the loan via GM mode.",
                            "Overdue Loan Payments",
                            JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }

    public boolean checkRetirementDefections() {
        if (getRetirementDefectionTracker().getRetirees().size() > 0) {
            Object[] options = { "Show Payout Dialog", "Cancel" };
            if (JOptionPane.YES_OPTION == JOptionPane
                    .showOptionDialog(
                            null,
                            "You have personnel who have left the unit or been killed in action but have not received their final payout.\nYou must deal with these payments before advancing the day.\nHere are some options:\n  - Sell off equipment to generate funds.\n  - Pay one or more personnel in equipment.\n  - Just cheat and use GM mode to edit the settlement.",
                            "Unresolved Final Payments",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE, null, options,
                            options[0])) {
                return true;
            }
        }
        return false;
    }

    public boolean checkYearlyRetirements() {
        if (getCampaignOptions().getUseAtB()
                && Utilities.getDaysBetween(getRetirementDefectionTracker()
                        .getLastRetirementRoll().getTime(), getDate()) == 365) {
            Object[] options = { "Show Retirement Dialog", "Not Now" };
            if (JOptionPane.YES_OPTION == JOptionPane
                    .showOptionDialog(
                            null,
                            "It has been a year since the last retirement/defection roll, and it is time to do another.",
                            "Retirement/Defection roll required",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE, null, options,
                            options[0])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the type of rating method as selected in the Campaign Options dialog.
     * Lazy-loaded for performance. Default is CampaignOpsReputation
     */
    public IUnitRating getUnitRating() {
        // if we switched unit rating methods,
        if (unitRating != null && (unitRating.getUnitRatingMethod() != getCampaignOptions().getUnitRatingMethod())) {
            unitRating = null;
        }

        if (unitRating == null) {
            UnitRatingMethod method = getCampaignOptions().getUnitRatingMethod();

            if (UnitRatingMethod.FLD_MAN_MERCS_REV.equals(method)) {
                unitRating = new FieldManualMercRevDragoonsRating(this);
            } else {
                unitRating = new CampaignOpsReputation(this);
            }
        }

        return unitRating;
    }

    public int getPeacetimeCost() {
        int cost = 0;

        cost += getPayRoll(getCampaignOptions().useInfantryDontCount());
        cost += getMonthlySpareParts();
        cost += getMonthlyFuel();
        cost += getMonthlyAmmo();
        return cost;
    }

    public long getMonthlySpareParts() {
        long partsCost = 0;

        for (Unit u : getUnits()) {
            if (u.isMothballed()) {
                continue;
            }
            partsCost += u.getSparePartsCost();
        }
        return partsCost;
    }

    public long getMonthlyFuel() {
        long fuelCost = 0;

        for (Unit u : getUnits()) {
            if (u.isMothballed()) {
                continue;
            }
            fuelCost += u.getFuelCost();
        }
        return fuelCost;
    }

    public long getMonthlyAmmo() {
        long ammoCost= 0;

        for (Unit u : getUnits()) {
            if (u.isMothballed()) {
                continue;
            }
            ammoCost += u.getAmmoCost();
        }
        return ammoCost;
    }

    @Override
    public int getTechIntroYear() {
        if (campaignOptions.limitByYear()) {
            return getGameYear();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getGameYear() {
        return calendar.get(Calendar.YEAR);
    }

    @Override
    public int getTechFaction() {
        return techFactionCode;
    }

    public void updateTechFactionCode() {
        if (campaignOptions.useFactionIntroDate()) {
            for (int i = 0; i < ITechnology.MM_FACTION_CODES.length; i++) {
                if (ITechnology.MM_FACTION_CODES[i].equals(factionCode)) {
                    techFactionCode = i;
                    UnitTechProgression.loadFaction(techFactionCode);
                    return;
                }
            }
            // If the tech progression data does not include the current faction,
            // use a generic.
            if (getFaction().isClan()) {
                techFactionCode = ITechnology.F_CLAN;
            } else if (getFaction().isPeriphery()) {
                techFactionCode = ITechnology.F_PER;
            } else {
                techFactionCode = ITechnology.F_IS;
            }
        } else {
            techFactionCode = ITechnology.F_NONE;
        }
        // Unit tech level will be calculated if the code has changed.
        UnitTechProgression.loadFaction(techFactionCode);
    }

    @Override
    public boolean useClanTechBase() {
        return getFaction().isClan();
    }

    @Override
    public boolean useMixedTech() {
        if (useClanTechBase()) {
            return campaignOptions.allowISPurchases();
        } else {
            return campaignOptions.allowClanPurchases();
        }
    }

    @Override
    public SimpleTechLevel getTechLevel() {
        for (SimpleTechLevel lvl : SimpleTechLevel.values()) {
            if (campaignOptions.getTechLevel() == lvl.ordinal()) {
                return lvl;
            }
        }
        return SimpleTechLevel.UNOFFICIAL;
    }

    @Override
    public boolean unofficialNoYear() {
        return false;
    }

    @Override
    public boolean useVariableTechLevel() {
        return campaignOptions.useVariableTechLevel();
    }

    @Override
    public boolean showExtinct() {
        return !campaignOptions.disallowExtinctStuff();
    }
}
