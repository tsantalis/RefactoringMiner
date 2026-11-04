"""Focused calculation facade and result-model checks."""

import gc
from collections.abc import Callable, Mapping
from concurrent.futures import ThreadPoolExecutor
from threading import Event, Lock, Thread
from time import perf_counter, sleep
from typing import Any, cast
from unittest.mock import patch

import chargefw
import numpy as np
import pytest
from chargefw._chargefw import core as _native_core

_GIL_OBSERVATION_DELAY_SECONDS = 0.001
_GIL_TEST_ATOM_COUNT = 500_000


def water(conformers: int = 1) -> chargefw.Molecule:
    coordinates = np.array(
        [
            [[0.0, 0.0, 0.0], [0.96, 0.0, 0.0], [-0.24, 0.93, 0.0]],
            [[0.0, 0.0, 0.1], [0.96, 0.0, 0.1], [-0.24, 0.93, 0.1]],
        ][:conformers]
    )
    return chargefw.Molecule(
        [8, 1, 1],
        bonds=np.array([[0, 1, 1], [0, 2, 1]], dtype=np.int32),
        coordinates=coordinates[0] if conformers == 1 else coordinates,
        source_name="water.sdf",
        record_index=2,
        record_id="water-record",
        atom_ids=["O", "H1", "H2"],
    )


FIXED_IONS_MMCIF = """data_fixed_groups
loop_
_atom_site.group_PDB
_atom_site.id
_atom_site.type_symbol
_atom_site.label_atom_id
_atom_site.label_alt_id
_atom_site.label_comp_id
_atom_site.label_asym_id
_atom_site.label_seq_id
_atom_site.pdbx_PDB_ins_code
_atom_site.Cartn_x
_atom_site.Cartn_y
_atom_site.Cartn_z
_atom_site.occupancy
_atom_site.B_iso_or_equiv
_atom_site.pdbx_formal_charge
_atom_site.auth_seq_id
_atom_site.auth_comp_id
_atom_site.auth_asym_id
_atom_site.auth_atom_id
_atom_site.pdbx_PDB_model_num
HETATM 1 O O . HOH W . ? 0 0 0 1 20 0 5 HOH W O 1
HETATM 2 H H1 . HOH W . ? 0.96 0 0 1 20 0 5 HOH W H1 1
HETATM 3 H H2 . HOH W . ? -0.24 0.93 0 1 20 0 5 HOH W H2 1
HETATM 4 Mg MG . MG I . ? 0 0 3 1 20 0 9 MG I MG 1
#
"""


def fixed_groups_molecules() -> chargefw.MoleculeCollection:
    return chargefw.io.parse(FIXED_IONS_MMCIF, format="mmcif", bonds="templates")


def calculate_formal(molecules: Any) -> chargefw.CalculationResult:
    return chargefw.calculate(molecules, method="formal", execution="full", threads=1)


def assess_formal(molecules: Any) -> chargefw.Assessment:
    return chargefw.assess(molecules, method="formal", execution="full", threads=1)


def run_while_python_thread_progresses[T](operation: Callable[[], T]) -> tuple[T, bool, float]:
    started = Event()
    begin = Event()
    observing = Event()
    progressed = Event()

    def observe() -> None:
        started.set()
        begin.wait()
        observing.set()
        sleep(_GIL_OBSERVATION_DELAY_SECONDS)
        progressed.set()

    worker = Thread(target=observe)
    worker.start()
    if not started.wait(timeout=1.0):
        worker.join(timeout=1.0)
        raise AssertionError("worker thread did not start")
    try:
        begin.set()
        if not observing.wait(timeout=1.0):
            raise AssertionError("worker thread did not begin observing")
        start = perf_counter()
        result = operation()
        operation_seconds = perf_counter() - start
        progressed_during_operation = progressed.is_set()
    finally:
        worker.join(timeout=1.0)
    if worker.is_alive():
        raise AssertionError("worker thread did not stop")
    return result, progressed_during_operation, operation_seconds


class RecordingObserver(chargefw.CalculationObserver):
    def __init__(self) -> None:
        self.events: list[chargefw.CalculationProgress] = []

    def on_progress(self, progress: chargefw.CalculationProgress) -> None:
        self.events.append(progress)


def test_ion_selections_are_immutable_native_catalog_tuples() -> None:
    common: tuple[str, ...] = chargefw.COMMON_IONS
    all_ions: tuple[str, ...] = chargefw.ALL_IONS
    assert isinstance(all_ions, tuple)
    assert set(common) < set(all_ions)
    assert len(all_ions) == len(set(all_ions))
    for selection in (common, all_ions):
        absent = chargefw.calculate(
            chargefw.io.parse(FIXED_IONS_MMCIF.split("HETATM 4")[0] + "#\n", format="mmcif"),
            method="formal",
            fixed_ions=selection,
        )
        assert absent.requested.fixed_ions == selection
        assert absent.plan is not None, "absent ion selection must retain the formal plan"
        assert absent.plan.fixed_ions is None


def test_all_ions_selects_noncommon_template_in_assessment_and_calculation() -> None:
    # CU1's atom name is CU and its template charge differs from the imported zero.
    text = FIXED_IONS_MMCIF.replace(
        "HETATM 4 Mg MG . MG I . ? 0 0 3 1 20 0 9 MG I MG 1",
        "HETATM 4 Cu CU . CU1 I . ? 0 0 3 1 20 0 9 CU1 I CU 1",
    )
    molecules = chargefw.io.parse(text, format="mmcif", bonds="templates")
    common = chargefw.calculate(molecules, method="formal", fixed_ions=chargefw.COMMON_IONS)
    assert common.plan is not None, (
        "unselected noncommon ion must leave formal calculation available"
    )
    assert common.plan.fixed_ions is None
    np.testing.assert_array_equal(common.assignments[0].values, [0.0] * 4)
    assessment = chargefw.assess(
        molecules,
        method="sqeqp",
        parameter_set="SQEqp_Schindler2021_CCD_gen",
        execution="full",
        fixed_ions=chargefw.ALL_IONS,
    )
    assert assessment.default_plan is not None, (
        "ALL_IONS must produce an SQEqp plan with the CU1 template"
    )
    result = chargefw.calculate(molecules, assessment.default_plan)
    assert result.requested.fixed_ions == chargefw.ALL_IONS
    assert result.assignments[0].values[3] == 1.0
    assert float(result.assignments[0].values[:3].sum()) == pytest.approx(0.0, abs=1e-12)
    np.testing.assert_array_equal(molecules[0].formal_charges, [0] * 4)
    assert result.plan is not None and result.plan.fixed_ions is not None, (
        "ALL_IONS must retain effective source provenance"
    )
    assert result.plan.fixed_ions.sources == (chargefw.FixedAtomCharge(0, 3, 1.0),)


def test_observer_receives_owned_execution_progress() -> None:
    observer = RecordingObserver()
    collection = chargefw.MoleculeCollection([chargefw.Molecule([1]), chargefw.Molecule([8, 1])])
    result = chargefw.calculate(
        collection,
        method="formal",
        execution="full",
        threads=1,
        observer=observer,
    )

    assert result.status == "success"
    assert [event.phase for event in observer.events] == [
        "computation_started",
        "target_started",
        "target_finished",
        "target_started",
        "target_finished",
        "computation_finished",
    ]
    started = observer.events[0]
    assert started.method_id == "formal"
    assert started.mode == "full"
    targets = [event for event in observer.events if event.phase == "target_started"]
    assert [event.target_index for event in targets] == [0, 1]
    assert [event.target_count for event in targets] == [2, 2]
    assert [event.molecule_index for event in targets] == [0, 1]
    assert [event.conformer_index for event in targets] == [None, None]
    assert all(event.elapsed_seconds >= 0.0 for event in observer.events)


def test_reduced_execution_reports_policy_and_fragment_progress() -> None:
    for mode in ("cutoff", "cover"):
        observer = RecordingObserver()
        result = chargefw.calculate(
            water(),
            method="eem",
            execution=cast(Any, mode),
            radius=8.0,
            threads=1,
            observer=observer,
        )

        assert result.status == "success"
        assert result.plan is not None, "successful calculation must report plan provenance"
        assert result.plan.policy == chargefw.ExecutionPolicy(mode=mode, radius=8.0)
        fragments = [event for event in observer.events if event.phase == "fragment_progress"]
        assert fragments
        assert fragments[-1].completed_fragment_count == fragments[-1].fragment_count
        assert fragments[-1].target_index == 0
        assert fragments[-1].target_count == 1


def test_fixed_ions_snapshot_resolve_and_reuse_plan() -> None:
    molecules = fixed_groups_molecules()
    assert molecules[0].bond_count == 2
    names = ["MG"]
    assessment = chargefw.assess(
        molecules,
        method="sqeqp",
        parameter_set="SQEqp_Schindler2021_CCD_gen",
        execution="full",
        fixed_ions=names,
    )
    names.append("CA")
    plan = assessment.default_plan
    assert plan is not None, "assessment must produce an SQEqp plan with the selected MG source"
    del assessment
    gc.collect()
    result = chargefw.calculate(molecules, plan)
    direct = chargefw.calculate(
        molecules,
        method="sqeqp",
        parameter_set="SQEqp_Schindler2021_CCD_gen",
        execution="full",
        fixed_ions=chargefw.COMMON_IONS,
    )
    assert result.requested.fixed_ions == ("MG",)
    assert result.assignments[0].values[3] == 2.0
    np.testing.assert_allclose(result.assignments[0].values, direct.assignments[0].values)
    assert molecules[0].formal_charges.tolist() == [0, 0, 0, 0]
    assert result.plan is not None and result.plan.fixed_ions is not None, (
        "executed plan must retain effective source provenance"
    )
    assert result.plan.fixed_ions.sources == (chargefw.FixedAtomCharge(0, 3, 2.0),)
    assert result.plan.fixed_ions == chargefw.FixedIons([chargefw.FixedAtomCharge(0, 3, 2.0)])
    with pytest.raises(AttributeError):
        cast(Any, result.plan.fixed_ions).sources = ()
    with pytest.raises(AttributeError):
        cast(Any, result.requested).fixed_ions = ("CA",)
    with pytest.raises(TypeError):
        chargefw.calculate(molecules, plan, fixed_ions=[])


def test_fixed_ions_validation_empty_and_method_support() -> None:
    molecules = fixed_groups_molecules()
    for invalid in ("MG", b"MG", ["MG", 12]):
        with pytest.raises(TypeError):
            chargefw.assess(molecules, fixed_ions=cast(Any, invalid))
    with pytest.raises(ValueError):
        chargefw.assess(molecules, fixed_ions=["mg"])
    with pytest.raises(ValueError):
        chargefw.assess(chargefw.MoleculeCollection([]), fixed_ions=["bad"])

    absent = chargefw.calculate(molecules, method="formal", fixed_ions=["CA"])
    assert absent.requested.fixed_ions == ("CA",)
    assert absent.plan is not None, "formal calculation must produce a plan"
    assert absent.plan.fixed_ions is None
    duplicate = chargefw.calculate(molecules, method="eem", fixed_ions=["MG", "MG"])
    assert duplicate.plan is not None and duplicate.plan.fixed_ions is not None, (
        "duplicate names must resolve to one fixed source"
    )
    assert len(duplicate.plan.fixed_ions.sources) == 1
    assert chargefw.RequestedCalculation().fixed_ions == ()

    plain = chargefw.calculate(water(), method="formal")
    empty = chargefw.calculate(water(), method="formal", fixed_ions=[])
    assert empty.requested.fixed_ions == ()
    np.testing.assert_array_equal(plain.assignments[0].values, empty.assignments[0].values)

    unsupported = chargefw.assess(
        molecules, method="qeq", parameter_set="QEq_original", fixed_ions=["MG"]
    )
    assert not unsupported.plans
    assert "unsupported_fixed_ions" in [
        issue.kind for rejection in unsupported.rejections for issue in rejection.issues
    ]
    with pytest.raises(ValueError):
        chargefw.assess(water(), fixed_ions=["MG"])


def test_observer_receives_parallel_target_progress() -> None:
    class CountingObserver(chargefw.CalculationObserver):
        def __init__(self) -> None:
            self.lock = Lock()
            self.completed = 0

        def on_progress(self, progress: chargefw.CalculationProgress) -> None:
            if progress.phase == "target_finished":
                with self.lock:
                    self.completed += 1

    molecule_count = 64
    observer = CountingObserver()
    result = chargefw.calculate(
        chargefw.MoleculeCollection(chargefw.Molecule([1]) for _ in range(molecule_count)),
        method="formal",
        execution="full",
        threads=2,
        observer=observer,
    )

    assert result.status == "success"
    assert observer.completed == molecule_count


def test_observer_can_cancel_a_reusable_plan() -> None:
    class CancellingObserver(chargefw.CalculationObserver):
        def __init__(self) -> None:
            self.cancel_requested = False
            self.phases: list[str] = []

        def on_progress(self, progress: chargefw.CalculationProgress) -> None:
            self.phases.append(progress.phase)
            if progress.phase == "target_started":
                self.cancel_requested = True

        def cancelled(self) -> bool:
            return self.cancel_requested

    cases = (
        (chargefw.MoleculeCollection([water()]), None),
        (fixed_groups_molecules(), ["MG"]),
    )
    for molecule, groups in cases:
        observer = CancellingObserver()
        options: dict[str, Any] = {
            "method": "eem" if groups is None else "sqeqp",
            "execution": "full",
            "fixed_ions": groups,
        }
        if groups is not None:
            options["parameter_set"] = "SQEqp_Schindler2021_CCD_gen"
        assessment = chargefw.assess(molecule, **options)
        plan = assessment.default_plan
        assert plan is not None, "assessment must produce a default plan"
        with pytest.raises(chargefw.CalculationCancelledError) as raised:
            chargefw.calculate(molecule, plan, threads=1, observer=observer)

        cancelled = raised.value.result
        assert cancelled.status == "cancelled"
        assert cancelled.assignments == ()
        assert observer.phases[0] == "computation_started"
        assert "target_started" in observer.phases
        assert observer.phases[-1] == "computation_finished"
        assert cancelled.requested.fixed_ions == tuple(groups or ())
        if groups is not None:
            assert cancelled.plan is not None
            assert cancelled.plan.fixed_ions is not None
            assert cancelled.plan.fixed_ions.sources == (chargefw.FixedAtomCharge(0, 3, 2.0),)

        result = chargefw.calculate(molecule, plan, threads=1)
        assert result.status == "success"
        assert len(result.assignments) == molecule[0].conformer_count
        assert all(
            assignment.values.shape == (molecule[0].atom_count,)
            for assignment in result.assignments
        )
        assert all(np.all(np.isfinite(assignment.values)) for assignment in result.assignments)


def test_observer_must_use_public_base_class() -> None:
    with pytest.raises(TypeError, match="CalculationObserver"):
        chargefw.calculate(water(), observer=cast(Any, object()))


def test_observer_callback_failure_does_not_change_calculation() -> None:
    class FailingObserver(chargefw.CalculationObserver):
        def on_progress(self, progress: chargefw.CalculationProgress) -> None:
            raise RuntimeError(f"failed during {progress.phase}")

    with patch("sys.unraisablehook") as unraisable:
        result = chargefw.calculate(
            water(),
            method="formal",
            execution="full",
            threads=1,
            observer=FailingObserver(),
        )

    assert result.status == "success"
    assert unraisable.called
    # The recorded exception owns its traceback, which otherwise retains this calculation frame
    # until interpreter shutdown and produces a false-positive Nanobind leak report.
    unraisable.reset_mock()


def test_default_calculation_selects_a_supported_plan() -> None:
    result = chargefw.calculate(water())
    assert result.status == "success"
    assert result.plan is not None


def test_assessment_and_full_calculation() -> None:
    molecule = water()
    assessment = chargefw.assess(molecule, method="eem", execution="full")
    assert assessment.plans
    assert all(plan.policy.mode == "full" for plan in assessment.plans)
    plan = assessment.default_plan
    assert plan is not None, "assessment must produce a default plan"
    assert plan.policy == chargefw.ExecutionPolicy(
        mode="full",
        radius=None,
    )
    assert plan.method.id == "eem"

    result = chargefw.calculate(molecule, plan)
    assert result.status == "success"
    assert len(result.assignments) == 1
    assignment = result.assignments[0]
    assert assignment.molecule_index == 0
    assert assignment.conformer_index == 0
    assert assignment.source.record_id == "water-record"
    assert assignment.atom_ids == ("O", "H1", "H2")
    assert assignment.values.dtype == np.dtype(np.float64)
    assert assignment.values.flags.c_contiguous
    assert not assignment.values.flags.writeable
    assert assignment.values.ctypes.data % assignment.values.dtype.alignment == 0
    with pytest.raises(ValueError):
        assignment.values.setflags(write=True)
    assert np.isclose(assignment.values.sum(), 0.0)
    assert np.asarray(assignment) is assignment.values
    assert np.asarray(assignment, dtype=np.float32).dtype == np.dtype(np.float32)
    assert result.requested.method == "eem"
    assert result.requested.execution == "full"
    executed = result.plan
    assert executed is not None, "successful calculation must report plan provenance"
    assert executed.method.id == "eem"
    assert executed.parameter_set == plan.parameter_set
    assert executed.policy.mode == "full"
    assert result.timings.applicability_seconds >= 0.0
    assert result.timings.computation_seconds >= 0.0
    with pytest.raises(TypeError):
        cast(Any, result.requested.options_by_method)["eem"] = {"unexpected": True}

    overridden_threads = chargefw.calculate(molecule, plan, threads=1)
    assert overridden_threads.requested.threads == 1


def test_plan_keeps_prepared_state_alive() -> None:
    molecule = water()
    plan = chargefw.assess(molecule, method="eem", execution="full").default_plan
    assert plan is not None, "assessment must produce a default plan"
    gc.collect()
    assert chargefw.calculate(molecule, plan).status == "success"


def test_plans_are_filtered_reusable_and_target_bound() -> None:
    molecule = water()
    assessment = chargefw.assess(molecule, method="eqeq")
    assert {candidate_plan.policy.mode for candidate_plan in assessment.plans} == {
        "full",
        "cutoff",
        "cover",
    }
    for candidate_plan in assessment.plans:
        assert chargefw.calculate(molecule, candidate_plan, threads=1).status == "success"

    cutoff = chargefw.assess(
        molecule,
        method="eqeq",
        execution="cutoff",
        radius=8.0,
    )
    assert cutoff.plans
    assert all(plan.policy.mode == "cutoff" for plan in cutoff.plans)

    default_plan = assessment.default_plan
    assert default_plan is not None, "assessment must produce a default plan"
    with pytest.raises(ValueError):
        chargefw.calculate(water(), default_plan)
    with pytest.raises(TypeError):
        chargefw.calculate(molecule, cast(Any, None))
    with pytest.raises(TypeError):
        chargefw.calculate(molecule, default_plan, method="eqeq")

    with ThreadPoolExecutor(max_workers=3) as executor:
        results = list(
            executor.map(
                lambda _: chargefw.calculate(molecule, default_plan, threads=1),
                range(3),
            )
        )
    assert all(result.status == "success" for result in results)


def test_plan_estimates_resources_for_a_thread_limit() -> None:
    molecules = [water(), water()]
    plan = chargefw.assess(molecules, method="eqeq", execution="full").default_plan
    assert plan is not None, "assessment must produce a default plan"

    serial = plan.estimate(threads=1)
    parallel = plan.estimate(threads=2)
    assert serial.memory_bytes > 0.0
    assert serial.operations > 0.0
    assert parallel.memory_bytes > serial.memory_bytes
    assert parallel.operations == serial.operations
    assert plan.estimate().memory_bytes >= serial.memory_bytes
    with pytest.raises(ValueError):
        plan.estimate(threads=-1)


def test_assignment_cardinality_and_mapping() -> None:
    geometry_independent = chargefw.calculate(
        chargefw.Molecule([8, 1, 1]),
        method="formal",
        execution="full",
    )
    assert geometry_independent.status == "success"
    assert len(geometry_independent.assignments) == 1
    assert geometry_independent.assignments[0].conformer_index is None
    assert geometry_independent.assignment(molecule=0) is geometry_independent.assignments[0]
    with pytest.raises(ValueError):
        geometry_independent.assignment(molecule=0, conformer=0)

    collection = chargefw.MoleculeCollection([water(2)])
    multi_result = chargefw.calculate(collection, method="qeq", execution="full")
    assert multi_result.status == "success"
    assert [item.conformer_index for item in multi_result.assignments] == [0, 1]
    assert multi_result.assignments_by_molecule == (multi_result.assignments,)
    assert multi_result.assignment(molecule=0, conformer=0) is multi_result.assignments[0]
    assert multi_result.assignment(molecule=0, conformer=1) is multi_result.assignments[1]
    with pytest.raises(ValueError):
        multi_result.assignment(molecule=0)

    mapped_collection = chargefw.MoleculeCollection(
        [
            chargefw.Molecule([1], source_name="first", record_id="record-a", atom_ids=["A"]),
            chargefw.Molecule(
                [8, 1],
                source_name="second",
                record_id="record-b",
                atom_ids=["B", "C"],
            ),
        ]
    )
    mapped_result = calculate_formal(mapped_collection)
    assert mapped_result.status == "success"
    assert [assignment.molecule_index for assignment in mapped_result.assignments] == [0, 1]
    assert [assignment.source.record_id for assignment in mapped_result.assignments] == [
        "record-a",
        "record-b",
    ]
    assert [assignment.atom_ids for assignment in mapped_result.assignments] == [("A",), ("B", "C")]
    assert mapped_result.assignments_by_molecule == (
        (mapped_result.assignments[0],),
        (mapped_result.assignments[1],),
    )


def test_public_charge_assignment_is_validated_and_comparable() -> None:
    source = chargefw.SourceIdentity("fixture", 1, "record")
    assignment = chargefw.ChargeAssignment(
        values=np.array([0.25, -0.25]),
        molecule_index=cast(Any, np.int64(2)),
        conformer_index=0,
        source=source,
        atom_ids=("A", "B"),
    )
    same = chargefw.ChargeAssignment(
        values=np.array([0.25, -0.25]),
        molecule_index=2,
        conformer_index=0,
        source=source,
        atom_ids=("A", "B"),
    )
    different = chargefw.ChargeAssignment(
        values=np.array([0.5, -0.5]),
        molecule_index=2,
        conformer_index=0,
        source=source,
        atom_ids=("A", "B"),
    )
    assert assignment == same
    assert assignment != different
    assert not assignment.values.flags.writeable
    with pytest.raises(ValueError):
        assignment.values.setflags(write=True)

    invalid_assignments = (
        (ValueError, {"values": [[0.0]], "atom_ids": ("A",)}),
        (ValueError, {"values": [np.nan], "atom_ids": ("A",)}),
        (ValueError, {"values": [0.0], "atom_ids": ()}),
        (
            ValueError,
            {"values": [0.0], "atom_ids": ("A",), "molecule_index": -1},
        ),
        (
            TypeError,
            {"values": [0.0], "atom_ids": ("A",), "source": "fixture"},
        ),
    )
    defaults: dict[str, Any] = {
        "molecule_index": 0,
        "conformer_index": None,
        "source": source,
    }
    for error_type, overrides in invalid_assignments:
        with pytest.raises(error_type):
            chargefw.ChargeAssignment(**(defaults | overrides))


def test_functional_api_supports_concurrent_calculations() -> None:
    collection = chargefw.MoleculeCollection([chargefw.Molecule([8, 1, 1])])
    with ThreadPoolExecutor(max_workers=4) as executor:
        futures = [executor.submit(calculate_formal, collection) for _ in range(4)]
    results = [future.result() for future in futures]
    assert all(result.status == "success" for result in results)
    assert [result.assignments[0].values.tolist() for result in results] == [[0.0, 0.0, 0.0]] * 4


def test_assessment_releases_the_gil() -> None:
    molecule = chargefw.Molecule([1] * _GIL_TEST_ATOM_COUNT)
    assessment, progressed, operation_seconds = run_while_python_thread_progresses(
        lambda: assess_formal(molecule)
    )
    assert assessment.plans
    assert operation_seconds > _GIL_OBSERVATION_DELAY_SECONDS
    assert progressed


def test_native_molecule_construction_releases_the_gil() -> None:
    atom_count = _GIL_TEST_ATOM_COUNT
    atomic_numbers = np.ones(atom_count, dtype=np.int64)
    formal_charges = np.zeros(atom_count, dtype=np.int64)
    bonds = np.empty((0, 3), dtype=np.int64)
    coordinates = np.empty((0, atom_count, 3), dtype=np.float64)
    atom_names = ("",) * atom_count
    molecule, progressed, operation_seconds = run_while_python_thread_progresses(
        lambda: _native_core._make_molecule(
            atomic_numbers,
            formal_charges,
            bonds,
            coordinates,
            atom_names,
            (),
            "gil-test",
        )
    )
    assert isinstance(molecule, _native_core._NativeMolecule)
    assert operation_seconds > _GIL_OBSERVATION_DELAY_SECONDS
    assert progressed


def test_execution_releases_the_gil() -> None:
    molecule = chargefw.Molecule([1] * _GIL_TEST_ATOM_COUNT)
    assessment = assess_formal(molecule)
    plan = assessment.default_plan
    assert plan is not None, "assessment must produce a default plan"
    result, progressed, operation_seconds = run_while_python_thread_progresses(
        lambda: chargefw.calculate(molecule, plan)
    )
    assert result.status == "success"
    assert operation_seconds > _GIL_OBSERVATION_DELAY_SECONDS
    assert progressed


def test_automatic_thresholds_and_explicit_full_warnings() -> None:
    automatic = chargefw.assess(
        water(),
        method="eem",
        cutoff_threshold=1,
        cover_threshold=100,
    )
    assert automatic.default_plan is not None, "assessment must produce a default plan"
    assert automatic.default_plan.policy.mode == "cutoff"
    assert any(
        rejection.policy is not None and rejection.policy.mode == "full"
        for rejection in automatic.rejections
    )

    explicit_full = chargefw.assess(
        water(),
        method="eem",
        execution="full",
        cutoff_threshold=1,
        cover_threshold=100,
    )
    assert explicit_full.default_plan is not None, "explicit full assessment must produce a plan"
    assert [warning.kind for warning in explicit_full.default_plan.warnings] == [
        "resource_threshold_exceeded"
    ]


def test_no_plan_result_and_typed_exception() -> None:
    molecule = chargefw.Molecule([8, 1, 1], bonds=[[0, 1, 1], [0, 2, 1]])
    assessment = chargefw.assess(
        molecule,
        method="qeq",
        execution="full",
    )
    assert not assessment.plans
    with pytest.raises(chargefw.NoExecutablePlanError) as context:
        chargefw.calculate(molecule, method="qeq", execution="full")
    result = context.value.result
    assert result.status == "no_executable_plan"
    assert result.assignments == ()
    assert result.rejections
    assert result.rejections[0].issues[0].kind == "missing_feature"


def test_nonfinite_coordinates_are_assessed_per_method() -> None:
    molecule = chargefw.Molecule(
        [8, 1, 1],
        bonds=[[0, 1, 1], [0, 2, 1]],
        coordinates=[[0.0, 0.0, 0.0], [np.nan, 0.0, 0.0], [-0.24, 0.93, 0.0]],
    )
    assert np.isnan(molecule.coordinates[0, 1, 0])

    formal_result = chargefw.calculate(molecule, method="formal", execution="full")
    assert formal_result.status == "success"

    assessment = chargefw.assess(
        molecule,
        method="qeq",
        parameter_set="QEq_original",
        execution="full",
    )
    assert not assessment.plans
    invalid_geometry = [
        issue
        for rejection in assessment.rejections
        for issue in rejection.issues
        if issue.kind == "invalid_geometry"
    ]
    assert len(invalid_geometry) == 1
    assert invalid_geometry[0].atom_index == 1
    assert invalid_geometry[0].conformer_index == 0


def test_invalid_selection_requests_raise_value_error() -> None:
    with pytest.raises(ValueError):
        chargefw.assess(water(), method="not-a-method")

    with pytest.raises(ValueError):
        chargefw.assess(water(), parameter_set="QEq_original")

    with pytest.raises(ValueError):
        chargefw.assess(water(), method="eem", parameter_set="not-a-parameter-set")

    with pytest.raises(ValueError):
        chargefw.assess(water(), method="eem", parameter_set="QEq_original")


def test_results_outlive_calculation_inputs() -> None:
    def calculate_owned_result() -> tuple[chargefw.CalculationResult, chargefw.Plan]:
        molecule = chargefw.Molecule(
            [8, 1, 1],
            source_name="owned",
            record_id="owned-record",
            atom_ids=["O", "H1", "H2"],
        )
        assessment = assess_formal(molecule)
        plan = assessment.default_plan
        assert plan is not None, "assessment must produce a default plan"
        return chargefw.calculate(molecule, plan), plan

    result, plan = calculate_owned_result()
    gc.collect()
    assert plan.policy.mode == "full"
    assert result.status == "success"
    assert result.assignments[0].source.record_id == "owned-record"
    assert result.assignments[0].atom_ids == ("O", "H1", "H2")
    np.testing.assert_array_equal(result.assignments[0].values, [0.0, 0.0, 0.0])

    values = result.assignments[0].values
    del result
    gc.collect()
    np.testing.assert_array_equal(values, [0.0, 0.0, 0.0])
    assert not values.flags.writeable


def test_invalid_options_are_rejected_early() -> None:
    invalid_options = (
        (ValueError, {"execution": "fast"}),
        (ValueError, {"parameter_matching": "guess"}),
        (TypeError, {"threads": True}),
        (TypeError, {"cutoff_threshold": False}),
        (ValueError, {"cover_threshold": np.iinfo(np.uintp).max + 1}),
    )
    for error_type, options in invalid_options:
        with pytest.raises(error_type):
            chargefw.assess(water(), **options)

    with pytest.raises(ValueError):
        chargefw.assess(water(), options={"iters": 2})
    with pytest.raises(ValueError):
        chargefw.assess(
            water(),
            method="peoe",
            options={"iters": 2},
            options_by_method={"peoe": {"iters": 2}},
        )


def test_method_option_overrides_are_validated_and_reported() -> None:
    molecule = chargefw.Molecule([8, 1, 1], bonds=[[0, 1, 1], [0, 2, 1]])
    result = chargefw.calculate(
        molecule,
        method="peoe",
        options={"iters": 2},
        execution="full",
    )
    assert result.status == "success"
    assert result.plan is not None, "successful calculation must report plan provenance"
    assert dict(result.plan.options) == {"initial_charges": "zero", "iters": 2}

    for options in (
        {"iters": 0},
        {"iters": 26},
        {"initial_charges": "unknown"},
        {"unknown": 1},
    ):
        with pytest.raises(ValueError):
            chargefw.assess(
                molecule,
                method="peoe",
                options=options,
                execution="full",
            )
    with pytest.raises(ValueError):
        chargefw.assess(
            molecule,
            method="peoe",
            options_by_method={"qeq": {"overlap_term": "Ohno"}},
            execution="full",
        )


def test_catalogs_and_descriptors_are_immutable_values() -> None:
    methods = chargefw.methods
    assert isinstance(methods, Mapping)
    assert "eem" in methods
    assert methods.get("eem") == methods["eem"]
    assert methods.get("not-a-method") is None
    assert tuple(methods) == tuple(method.id for method in methods.values())
    assert methods["eem"].id == "eem"
    with pytest.raises(KeyError):
        methods["not-a-method"]
    with pytest.raises(TypeError):
        cast(Any, methods)["unexpected"] = methods["eem"]
    with pytest.raises(TypeError):
        del cast(Any, methods)["eem"]

    eem = methods["eem"]
    assert tuple(eem.parameter_sets) == tuple(chargefw.parameter_sets.for_method("eem"))
    peoe = methods["peoe"]
    assert isinstance(peoe.options, Mapping)
    assert peoe.options["iters"].id == "iters"

    assert isinstance(chargefw.parameter_sets, Mapping)
    parameter_set = next(iter(eem.parameter_sets.values()))
    assert chargefw.parameter_sets[parameter_set.id] == parameter_set

    qeq = chargefw.parameter_sets["QEq_original"]
    assert {key.classifier for key in qeq.atom_keys} == {"plain"}
    assert qeq.bond_keys == ()
    peptide = chargefw.parameter_sets["SQEqp_Schindler2021_PUB_pept"]
    assert chargefw.AtomParameterKey("C", "bonded", "COO") in peptide.atom_keys
    bond = peptide.bond_keys[0]
    assert bond.classifier == "bo"
    assert all(atom.classifier == "bonded" for atom in bond.atoms)
    result = chargefw.calculate(
        water(),
        method=eem,
        parameter_set=parameter_set,
        execution="full",
    )
    assert result.status == "success"
    executed = result.plan
    assert executed is not None, "successful calculation must report plan provenance"
    assert executed.parameter_set == parameter_set
