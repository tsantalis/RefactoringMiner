/*******************************************************************************
 * Copyright 2013 EMBL-EBI
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package htsjdk.samtools.cram.encoding.reader;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.CRAMException;
import htsjdk.samtools.cram.encoding.readfeatures.BaseQualityScore;
import htsjdk.samtools.cram.encoding.readfeatures.Bases;
import htsjdk.samtools.cram.encoding.readfeatures.Deletion;
import htsjdk.samtools.cram.encoding.readfeatures.HardClip;
import htsjdk.samtools.cram.encoding.readfeatures.InsertBase;
import htsjdk.samtools.cram.encoding.readfeatures.Insertion;
import htsjdk.samtools.cram.encoding.readfeatures.Padding;
import htsjdk.samtools.cram.encoding.readfeatures.ReadBase;
import htsjdk.samtools.cram.encoding.readfeatures.ReadFeature;
import htsjdk.samtools.cram.encoding.readfeatures.RefSkip;
import htsjdk.samtools.cram.encoding.readfeatures.Scores;
import htsjdk.samtools.cram.encoding.readfeatures.SoftClip;
import htsjdk.samtools.cram.encoding.readfeatures.Substitution;
import htsjdk.samtools.cram.structure.AlignmentSpan;
import htsjdk.samtools.cram.structure.CramCompressionRecord;
import htsjdk.samtools.cram.structure.ReadTag;
import htsjdk.samtools.cram.structure.Slice;
import htsjdk.samtools.util.RuntimeIOException;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A reader that only keeps track of alignment spans. The intended use is for
 * CRAI index.
 * 
 * @author vadim
 *
 */
public class RefSeqIdReader extends AbstractReader {
	/**
	 * Reference sequence id set by default
	 */
	private final int globalReferenceSequenceId;

	/**
	 * Alignment start to start counting from
	 */
	private int alignmentStart;
	private ValidationStringency validationStringency;
	/**
	 * For diagnostic purposes
	 */
	private int recordCounter = 0;

	/**
	 * Single record to use for capturing read fields:
 	 */
	private final CramCompressionRecord cramRecord = new CramCompressionRecord();

	/**
	 * Detected sequence spans
	 */
	private final Map<Integer, AlignmentSpan> spans = new HashMap<>();

	public RefSeqIdReader(final int seqId, final int alignmentStart, ValidationStringency validationStringency) {
		super();
		this.globalReferenceSequenceId = seqId;
		this.alignmentStart = alignmentStart;
		this.validationStringency = validationStringency;
	}

	public Map<Integer, AlignmentSpan> getReferenceSpans() {
		return spans;
	}

	public void read() {
		cramRecord.sequenceId = globalReferenceSequenceId;
		try {
			cramRecord.flags = bitFlagsCodec.readData();
			cramRecord.compressionFlags = compressionBitFlagsCodec.readData();
			if (refId == Slice.MULTI_REFERENCE)
				cramRecord.sequenceId = refIdCodec.readData();
			else
				cramRecord.sequenceId = refId;

			cramRecord.readLength = readLengthCodec.readData();
			if (APDelta) {
				cramRecord.alignmentDelta = alignmentStartCodec.readData();
				alignmentStart += cramRecord.alignmentDelta;
			}
			else {
				cramRecord.alignmentStart = alignmentStartCodec.readData();
				alignmentStart = cramRecord.alignmentStart;
			}

			cramRecord.readGroupID = readGroupCodec.readData();

			if (captureReadNames)
				cramRecord.readName = new String(readNameCodec.readData(), charset);

			// mate record:
			if (cramRecord.isDetached()) {
				cramRecord.mateFlags = mateBitFlagCodec.readData();
				if (!captureReadNames)
					cramRecord.readName = new String(readNameCodec.readData(), charset);

				cramRecord.mateSequenceID = mateReferenceIdCodec.readData();
				cramRecord.mateAlignmentStart = mateAlignmentStartCodec.readData();
				cramRecord.templateSize = insertSizeCodec.readData();
				detachedCount++;
			} else if (cramRecord.isHasMateDownStream())
				cramRecord.recordsToNextFragment = distanceToNextFragmentCodec.readData();

			final Integer tagIdList = tagIdListCodec.readData();
			final byte[][] ids = tagIdDictionary[tagIdList];
			if (ids.length > 0) {
				final int tagCount = ids.length;
				cramRecord.tags = new ReadTag[tagCount];
				for (int i = 0; i < ids.length; i++) {
					final int id = ReadTag.name3BytesToInt(ids[i]);
					final DataReader<byte[]> dataReader = tagValueCodecs.get(id);
					final ReadTag tag = new ReadTag(id, dataReader.readData(), validationStringency);
					cramRecord.tags[i] = tag;
				}
			}

			if (!cramRecord.isSegmentUnmapped()) {
				// reading read features:
				final int size = numberOfReadFeaturesCodec.readData();
				int prevPos = 0;
				final java.util.List<ReadFeature> readFeatures = new LinkedList<>();
				cramRecord.readFeatures = readFeatures;
				for (int i = 0; i < size; i++) {
					final Byte operator = readFeatureCodeCodec.readData();

					final int pos = prevPos + readFeaturePositionCodec.readData();
					prevPos = pos;

					switch (operator) {
					case ReadBase.operator:
						final ReadBase readBase = new ReadBase(pos, baseCodec.readData(), qualityScoreCodec.readData());
						readFeatures.add(readBase);
						break;
					case Substitution.operator:
						final Substitution substitution = new Substitution();
						substitution.setPosition(pos);
						final byte code = baseSubstitutionCodec.readData();
						substitution.setCode(code);
						readFeatures.add(substitution);
						break;
					case Insertion.operator:
						final Insertion insertion = new Insertion(pos, insertionCodec.readData());
						readFeatures.add(insertion);
						break;
					case SoftClip.operator:
						final SoftClip softClip = new SoftClip(pos, softClipCodec.readData());
						readFeatures.add(softClip);
						break;
					case HardClip.operator:
						final HardClip hardCLip = new HardClip(pos, hardClipCodec.readData());
						readFeatures.add(hardCLip);
						break;
					case Padding.operator:
						final Padding padding = new Padding(pos, paddingCodec.readData());
						readFeatures.add(padding);
						break;
					case Deletion.operator:
						final Deletion deletion = new Deletion(pos, deletionLengthCodec.readData());
						readFeatures.add(deletion);
						break;
					case RefSkip.operator:
						final RefSkip refSkip = new RefSkip(pos, refSkipCodec.readData());
						readFeatures.add(refSkip);
						break;
					case InsertBase.operator:
						final InsertBase insertBase = new InsertBase(pos, baseCodec.readData());
						readFeatures.add(insertBase);
						break;
					case BaseQualityScore.operator:
						final BaseQualityScore baseQualityScore = new BaseQualityScore(pos,
								qualityScoreCodec.readData());
						readFeatures.add(baseQualityScore);
						break;
					case Bases.operator:
						final Bases bases = new Bases(pos, basesCodec.readData());
						readFeatures.add(bases);
						break;
					case Scores.operator:
						final Scores scores = new Scores(pos, scoresCodec.readData());
						readFeatures.add(scores);
						break;
					default:
						throw new RuntimeException("Unknown read feature operator: " + operator);
					}
				}

				// mapping quality:
				cramRecord.mappingQuality = mappingScoreCodec.readData();
				if (cramRecord.isForcePreserveQualityScores()) {
					cramRecord.qualityScores = qualityScoresCodec.readDataArray(cramRecord.readLength);
				}
			} else {
				if (cramRecord.isUnknownBases()) {
					cramRecord.readBases = SAMRecord.NULL_SEQUENCE;
					cramRecord.qualityScores = SAMRecord.NULL_QUALS;
				} else {
					final byte[] bases = new byte[cramRecord.readLength];
					for (int i = 0; i < bases.length; i++)
						bases[i] = baseCodec.readData();
					cramRecord.readBases = bases;

					if (cramRecord.isForcePreserveQualityScores()) {
						cramRecord.qualityScores = qualityScoresCodec.readDataArray(cramRecord.readLength);
					}
				}
			}

			recordCounter++;

		} catch (final IOException e) {
			throw new RuntimeIOException(e);
		}

		if (!spans.containsKey(cramRecord.sequenceId)) {
			spans.put(cramRecord.sequenceId, new AlignmentSpan(alignmentStart, cramRecord.readLength));
		} else
			spans.get(cramRecord.sequenceId).addSingle(alignmentStart, cramRecord.readLength);
	}
}
